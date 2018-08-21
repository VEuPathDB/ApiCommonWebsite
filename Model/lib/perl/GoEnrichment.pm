package ApiCommonWebsite::Model::GoEnrichment;

use ApiCommonWebsite::Model::AbstractEnrichment;
@ISA = (ApiCommonWebsite::Model::AbstractEnrichment);

use strict;
use File::Basename;

sub new {
  my ($class)  = @_;

  my $self = {};
  bless( $self, $class );
  return $self;
}

sub run {
  my ($self, $outputFile, $geneResultSql, $modelName, $pValueCutoff, $subOntology, $evidCodes, $goSubset, $wordcloudFile, $secondOutputFile) = @_;

  die "Second argument must be an SQL select statement that returns the Gene result\n" unless $geneResultSql =~ m/select/i;
  die "Fourth argument must be a p-value between 0 and 1\n" unless $pValueCutoff > 0 && $pValueCutoff <= 1;

#  $self->{sources} = $sources;
  $self->{evidCodes} = $evidCodes;
  $self->{subOntology} = $subOntology;
  $self->{goSubset} = $goSubset;
  $self->SUPER::run($outputFile, $geneResultSql, $modelName, $pValueCutoff, $secondOutputFile);
}

sub getAnnotatedGenesCountBgd {
  my ($self, $dbh, $taxonId) = @_;

  my $sql = "
select count(distinct gts.gene_source_id)
from apidbTuning.GoTermSummary gts
where gts.taxon_id = $taxonId
  and gts.is_not is null
  and gts.evidence_category in ($self->{evidCodes})
  -- include a row only if either it's a GO Slim term or we aren't restricting to GO Slim terms
  and ($self->{goSubset} = 'No' or gts.is_go_slim = '1')
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for bgd annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getAnnotatedGenesCountResult {
  my ($self, $dbh, $geneResultSql) = @_;

  my $sql = "
select count(distinct gts.gene_source_id)
from apidbTuning.GoTermSummary gts,
     ($geneResultSql) r
where gts.gene_source_id = r.source_id
  and gts.is_not is null
  and gts.evidence_category in ($self->{evidCodes})
  -- include a row only if either it's a GO Slim term or we aren't restricting to GO Slim terms
  and ($self->{goSubset} = 'No' or gts.is_go_slim = '1')
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for result annotated genes count\n" unless $geneCount;
  return $geneCount;
}
sub getAnnotatedGenesListResult {
    my ($self, $dbh, $geneResultSql) = @_;

  my $sql = "
select distinct gts.gene_source_id
from apidbTuning.GoTermSummary gts,
     ($geneResultSql) r
where gts.gene_source_id = r.source_id
  and gts.is_not is null
  and gts.evidence_category in ($self->{evidCodes})
  -- include a row only if either it's a GO Slim term or we aren't restricting to GO Slim terms
  and ($self->{goSubset} = 'No' or gts.is_go_slim = '1')
";

    my $stmt = $self->runSql($dbh, $sql);
    my ($geneList) = $stmt->fetchrow_array();
    die "Got null gene count for result annotated genes count\n" unless $geneList;
    return $geneList;
}


sub getDataListSql {
  my ($self, $taxonId, $geneResultSql, $dbh) = @_;
  $dbh->{LongReadLen} = 66000;
  $dbh->{LongTruncOk} = 1;
return "
select bgd.go_id, bgdcnt, resultcnt, resultlist, round(100*resultcnt/bgdcnt, 1) as pct_of_bgd, bgd.name
from (select gts.go_id, count(distinct gts.gene_source_id) as bgdcnt,
             max(gts.go_term_name) as name
      from apidbTuning.GoTermSummary gts
      where gts.taxon_id = $taxonId
        and gts.ontology = '$self->{subOntology}'
        and gts.evidence_category in ($self->{evidCodes})
        and ($self->{goSubset} = 'No' or gts.is_go_slim = '1')
        and gts.is_not is null
      group by gts.go_id
     ) bgd,
     (select gts.go_id, count(distinct gts.gene_source_id) as resultcnt
      from apidbTuning.GoTermSummary gts,
           ($geneResultSql) r
      where gts.gene_source_id = r.source_id
        and gts.ontology = '$self->{subOntology}'
        and gts.evidence_category in ($self->{evidCodes})
        and ($self->{goSubset} = 'No' or gts.is_go_slim = '1')
        and gts.is_not is null
      group by gts.go_id
     ) rslt,
     (select gts.go_id, rtrim(xmlagg(xmlelement(e,gts.gene_source_id,',').extract('//text()') order by gts.gene_source_id).GetClobVal(),',') AS resultlist
      from apidbTuning.GoTermSummary gts,
           ($geneResultSql) r
      where gts.gene_source_id = r.source_id
        and gts.ontology = '$self->{subOntology}'
        and gts.evidence_category in ($self->{evidCodes})
        and ($self->{goSubset} = 'No' or gts.is_go_slim = '1')
        and gts.is_not is null
      group by gts.go_id
     ) rsltl
where bgd.go_id = rslt.go_id
  and rslt.go_id = rsltl.go_id
  and bgd.go_id = rsltl.go_id
";
}

sub usage {
  my $this = basename($0);

  die "
Find pathways that are enriched in the provided set of Genes.

Usage: $this outputFile sqlToFindGeneList modelName pValueCutoff subOntologyName annotationSources

Where:
  sqlToFindGeneList:    a select statement that will return all the rows in the db containing the genes result. Must have a source_id column.
  pValueCutoff:         the p-value exponent to use as a cutoff.  terms with a larger exponent are not returned
  outputFile:           the file in which to write results
  modelName:            eg, PlasmoDB.  Used to find the database connection.
  subOntologyName:      'Molecular Function' etc
  annotationSources:    a list of annotation sources in format compatible with an sql in clause. only include annotation that comes from these one or more sources.  (Eg, GeneDB, InterproScan).

The gene result must only include genes from a single taxon.  It is an error otherwise.

The output file is tab-delimited, with these columns (sorted by e-value)
      - Gene Ontology ID,
      - Gene Ontology Term,
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

1;
