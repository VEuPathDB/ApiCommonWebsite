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
SELECT count (distinct ga.source_id)
         from   dots.Transcript t, dots.translatedAaFeature taf, sres.enzymeClass ec,
               dots.aaSequenceEnzymeClass asec, ApidbTuning.GeneAttributes ga,
               apidb.pathwaynode pn
        where  ga.na_feature_id = t.parent_id
        AND    t.na_feature_id = taf.na_feature_id
        AND    taf.aa_sequence_id = asec.aa_sequence_id
        AND    asec.enzyme_class_id = ec.enzyme_class_id
        and    pn.display_label = ec.ec_number
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for bgd annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getAnnotatedGenesCountResult {
  my ($self, $dbh, $geneResultSql) = @_;

  my $sql = "
SELECT count (distinct ga.source_id)
         from   dots.Transcript t, dots.translatedAaFeature taf, sres.enzymeClass ec,
               dots.aaSequenceEnzymeClass asec, ApidbTuning.GeneAttributes ga,
               apidb.pathwaynode pn,
               ($geneResultSql) r
        where  ga.na_feature_id = t.parent_id
        AND    t.na_feature_id = taf.na_feature_id
        AND    taf.aa_sequence_id = asec.aa_sequence_id
        AND    asec.enzyme_class_id = ec.enzyme_class_id
        and    pn.display_label = ec.ec_number
        and    ga.source_id = r.source_id
";

  my $stmt = $self->runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for result annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getDataSql {
  my ($self, $taxonId, $geneResultSql) = @_;

return "
select distinct bgd.source_id, bgdcnt, resultcnt, round(100*resultcnt/bgdcnt, 1) as pct_of_bgd, bgd.name
from
 (SELECT  p.source_id,  count (distinct ga.source_id) as bgdcnt, p.name
        from   dots.Transcript t, dots.translatedAaFeature taf, sres.enzymeClass ec,
               dots.aaSequenceEnzymeClass asec, ApidbTuning.GeneAttributes ga,
               apidb.pathwaynode pn, apidb.pathway p
        where  ga.na_feature_id = t.parent_id
        and    ga.taxon_id = $taxonId
        AND    t.na_feature_id = taf.na_feature_id
        AND    taf.aa_sequence_id = asec.aa_sequence_id
        AND    asec.enzyme_class_id = ec.enzyme_class_id
        and    pn.display_label = ec.ec_number
        and    pn.parent_id = p.pathway_id
        group by p.source_id, p.name
   ) bgd,
   (SELECT  p.source_id,  count (distinct ga.source_id) as resultcnt
        from   dots.Transcript t, dots.translatedAaFeature taf, sres.enzymeClass ec,
               dots.aaSequenceEnzymeClass asec, ApidbTuning.GeneAttributes ga,
               apidb.pathwaynode pn, apidb.pathway p,
               ($geneResultSql) r
        where  ga.na_feature_id = t.parent_id
        AND    t.na_feature_id = taf.na_feature_id
        AND    taf.aa_sequence_id = asec.aa_sequence_id
        AND    asec.enzyme_class_id = ec.enzyme_class_id
        and    pn.display_label = ec.ec_number
        and    pn.parent_id = p.pathway_id
        and    ga.source_id = r.source_id
        group by p.source_id
 ) rslt
where bgd.source_id = rslt.source_id
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
