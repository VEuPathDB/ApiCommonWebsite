package ApiCommonWebsite::Model::PathwaysEnrichment;

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
  my ($self, $outputFile, $geneResultSql, $modelName, $pValueCutoff, $source) = @_;

  die "Second argument must be an SQL select statement that returns the Gene result\n" unless $geneResultSql =~ m/select/i;
  die "Fourth argument must be a p-value between 0 and 1\n" unless $pValueCutoff > 0 && $pValueCutoff <= 1;

  $self->SUPER::run($outputFile, $geneResultSql, $modelName, $pValueCutoff, $source);
}

sub getAnnotatedGenesCountBgd {
  my ($self, $dbh, $taxonId) = @_;

  my $sql = "
SELECT count (distinct gp.gene_source_id)
         from    apidbtuning.genepathway gp, ApidbTuning.GeneAttributes ga
        where  ga.taxon_id = $taxonId
        AND    gp.gene_source_id = ga.source_id
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for bgd annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getAnnotatedGenesCountResult {
  my ($self, $dbh, $geneResultSql) = @_;

  my $sql = "
SELECT count (distinct gp.gene_source_id)
         from  apidbtuning.genepathway gp,
               ($geneResultSql) r
        where  gp.gene_source_id = r.source_id
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for result annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getDataSql {
  my ($self, $taxonId, $geneResultSql) = @_;

return "
select distinct bgd.pathway_source_id, bgdcnt, resultcnt, round(100*resultcnt/bgdcnt, 1) as pct_of_bgd, bgd.pathway_name
from
 (SELECT  gp.pathway_source_id,  count (distinct gp.gene_source_id) as bgdcnt, gp.pathway_name
        from   apidbtuning.genepathway gp, ApidbTuning.GeneAttributes ga
        where  ga.taxon_id = $taxonId
         and   gp.gene_source_id = ga.source_id
        group by gp.pathway_source_id, gp.pathway_name
   ) bgd,
   (SELECT  gp.pathway_source_id,  count (distinct gp.gene_source_id) as resultcnt
        from   ApidbTuning.GenePathway gp,
               ($geneResultSql) r
        where  gp.gene_source_id = r.source_id
        group by gp.pathway_source_id
 ) rslt
where bgd.pathway_source_id = rslt.pathway_source_id
";
}

sub usage {
  my $this = basename($0);

  die "
Find pathways that are enriched in the provided set of Genes.

Usage: $this outputFile sqlToFindGeneList modelName pValueCutoff 

Where:
  sqlToFindGeneList:    a select statement that will return all the rows in the db containing the genes result. Must have a source_id column.
  pValueCutoff:         the p-value exponent to use as a cutoff.  terms with a larger exponent are not returned
  outputFile:           the file in which to write results
  modelName:            eg, PlasmoDB.  Used to find the database connection.

The gene result must only include genes from a single taxon.  It is an error otherwise.

The output file is tab-delimited, with these columns (sorted by e-value)
      - Pathway ID,
      - Pathway name,
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
