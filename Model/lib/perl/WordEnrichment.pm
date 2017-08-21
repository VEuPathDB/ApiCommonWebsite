package ApiCommonWebsite::Model::WordEnrichment;

use ApiCommonWebsite::Model::AbstractEnrichment;
@ISA = (ApiCommonWebsite::Model::AbstractEnrichment);

use strict;
use DBI;
use File::Basename;
use WDK::Model::ModelConfig;
use IPC::Open2;

sub new {
  my ($class)  = @_;

  my $self = {};
  bless( $self, $class );
  return $self;
}

sub run {
  my ($self, $outputFile, $geneResultSql, $modelName, $pValueCutoff) = @_;

  die "Second argument must be an SQL select statement that returns the Gene result\n" unless $geneResultSql =~ m/select/i;
  die "Fourth argument must be a p-value between 0 and 1\n" unless $pValueCutoff > 0 && $pValueCutoff <= 1;


  my $c = new WDK::Model::ModelConfig($modelName);

  my $dbh = DBI->connect($c->getAppDb->getDbiDsn, $c->getAppDb->getLogin, $c->getAppDb->getPassword) or die DBI::errstr;

  my $taxonId = $self->SUPER::getTaxonId($dbh, $geneResultSql);

  my $annotatedGenesBgd = $self->getAnnotatedGenesCountBgd($dbh, $taxonId);
  my $annotatedGenesResult = $self->getAnnotatedGenesCountResult($dbh, $geneResultSql);

  # get query to get back table to feed to python.
  # the columns are:  goId, bgdGeneCount, resultSetGeneCount
  my $dataSql = $self->getDataSql($taxonId, $geneResultSql);

  $self->getEnrichment($dbh, $outputFile, $annotatedGenesBgd, $annotatedGenesResult, $dataSql, $pValueCutoff);

  $dbh->disconnect or warn "Disconnection failed: $DBI::errstr\n";


}

sub getAnnotatedGenesCountBgd {
  my ($self, $dbh, $taxonId) = @_;

  my $sql = "
SELECT count (distinct gw.source_id)
         from  apidbtuning.GeneWord gw
        where  gw.taxon_id = $taxonId
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for bgd annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getAnnotatedGenesCountResult {
  my ($self, $dbh, $geneResultSql) = @_;

  my $sql = "
SELECT count (distinct gw.source_id)
         from  apidbtuning.GeneWord gw,
               ($geneResultSql) r
        where  gw.source_id = r.source_id
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for result annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getDataSql {
  my ($self, $taxonId, $geneResultSql) = @_;

return "
select distinct bgd.word, bgdcnt, resultcnt, round(100*resultcnt/bgdcnt, 1) as pct_of_bgd, bgd.descrip
from
 (SELECT  gw.word ,  count (distinct gw.source_id) as bgdcnt, '' as descrip
        from  apidbtuning.GeneWord gw
        where  gw.taxon_id = $taxonId
        group by gw.word
   ) bgd,
   (SELECT  gw.word,  count (distinct gw.source_id) as resultcnt
        from  apidbtuning.GeneWord gw,
               ($geneResultSql) r
        where  gw.source_id = r.source_id
        group by gw.word
 ) rslt
where bgd.word = rslt.word
";
}

sub usage {
  my $this = basename($0);

  die "
Find words from the product field that are enriched in the provided set of Genes.

Usage: $this outputFile sqlToFindGeneList modelName pValueCutoff 

Where:
  sqlToFindGeneList:    a select statement that will return all the rows in the db containing the genes result. Must have a source_id column.
  pValueCutoff:         the p-value exponent to use as a cutoff.  terms with a larger exponent are not returned
  outputFile:           the file in which to write results
  modelName:            eg, PlasmoDB.  Used to find the database connection.

The gene result must only include genes from a single taxon.  It is an error otherwise.

The output file is tab-delimited, with these columns (sorted by e-value)
      - word,
      - null,
      - Number of genes with this term in this organism,
      - Number of genes with this term in your result,
      - Percentage of genes in the organism with this term that are present in your result,
      - Ratio of the fraction of genes annotated by the term in result set to fraction of annotated genes in the organism,
      - Odds ratio statistic from the Fisher's exact test,
      - P-value from Fisher's exact test,
      - Benjamini-Hochberg FDR,
      - Bonferroni adjusted p-value

";

}

sub getEnrichment {
  my ($self, $dbh, $outputFile, $annotatedGenesBgd, $annotatedGenesResult, $dataSql, $pValueCutoff) = @_;

  my $cmd = "enrichmentAnalysis $pValueCutoff $annotatedGenesBgd $annotatedGenesResult";

  local (*Reader, *Writer);
  my $pid = open2(\*Reader, \*Writer, $cmd);

  my $stmt = $self->runSql($dbh, $dataSql);

  while ( my($annotationId, $geneCountBgd, $geneCountResult, $pctOfBgd, $annotationName) = $stmt->fetchrow_array()) {
#    print STDERR "$annotationId\t$geneCountBgd\t$geneCountResult\t$pctOfBgd\t$annotationName\n";
    print Writer "$annotationId\t$geneCountBgd\t$geneCountResult\t$pctOfBgd\t$annotationName\n";
  }

  close Writer;

  open(OUT, ">$outputFile") || die "Can't open '$outputFile' for writing\n";

  print OUT join("\t", "ID", "Name", "Bgd count", "Result count", "Pct of bgd", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni") . "\n";
  while(<Reader>) {
    chomp;
    my ($foldEnrichment, $oddsRatio, $percentOfResult, $pValue, $benjamini, $bonferroni, $termId, $bgdCount, $resultCount, $pctOfBgd, $annotationName) = split(/\t/);
    print OUT join("\t", $termId, $annotationName, $bgdCount, $resultCount, $pctOfBgd, $foldEnrichment, $oddsRatio, $pValue, $benjamini, $bonferroni) . "\n";
  }
  close Reader;
  waitpid($pid, 0);
  my $s = $? >> 8;
  die "Failed running command '$cmd'" if $s;
}


1;
