package ApiCommmonWebsite::Model::GoEnrichment;

use ApiCommonWebsite::Model::AbstractEnrichment;
@ISA = (ApiCommonWebsite::Model::AbstractEnrichment);

use strict;

sub new {
  my ($class)  = @_;

  my $self = {};
  bless( $self, $class );
  return $self;
}

sub run {
  my ($self, $outputFile, $geneResultSql, $modelName, $pValueCutoff, $subOntology, $sources, $evidCodes) = @_;

  die "Second argument must be an SQL select statement that returns the Gene result\n" unless $geneResultSql =~ m/select/i;
  die "Fourth argument must be a p-value between 0 and 1\n" unless $pValueCutoff > 0 && $pValueCutoff <= 1;

  $self->{sources} = $sources;
  $self->{evidCodes} = $evidCodes;
  $self->{subOntology} = $subOntology;
  super::run($outputFile, $geneResultSql, $modelName, $pValueCutoff);
}

sub getAnnotatedGenesCountBgd {
  my ($self, $dbh, $taxonId) = @_;

  my $sql = "
SELECT count(distinct ga.source_id)
FROM ApidbTuning.GoTermSummary gts, ApidbTuning.GeneAttributes ga
where ga.taxon_id = $taxonId
  and gts.source_id = ga.source_id
  and gts.is_not is null
  and gts.source in ($self->{sources})
  and gts.evidence_code in ($self->{evidCodes})
";

  my $stmt = runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for bgd annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getAnnotatedGenesCountResult {
  my ($self, $dbh, $geneResultSql) = @_;

  my $sql = "
SELECT count(distinct gts.source_id)
FROM ApidbTuning.GoTermSummary gts,
     ($geneResultSql) r
where gts.source_id = r.source_id
  and gts.is_not is null
  and gts.source in ($self->{sources})
  and gts.evidence_code in ($self->{evidCodes})
";

  my $stmt = runSql($dbh, $sql);
  my ($geneCount) = $stmt->fetchrow_array();
  die "Got null gene count for result annotated genes count\n" unless $geneCount;
  return $geneCount;
}

sub getDataSql {
  my ($self, $taxonId, $geneResultSql) = @_;

return "
select distinct bgd.go_id, bgdcnt, resultcnt, round(100*resultcnt/bgdcnt, 1) as pct_of_bgd, bgd.name
from
 (SELECT gt.go_id, count(distinct gts.source_id) as bgdcnt, gt.name
            FROM apidbtuning.geneattributes gf,
                 apidbtuning.gotermsummary gts,
                 sres.GoTerm gt,
                 sres.GoRelationship gr,
                 sres.GoRelationshipType grt
            WHERE gf.taxon_id = $taxonId
              AND gts.source_id = gf.source_id
              AND gts.ontology = '$self->{subOntology}'
              AND gts.source in ($self->{sources})
              AND gts.evidence_code in ($self->{evidCodes})
              AND gts.is_not is null
              AND gr.child_term_id = gts.go_term_id
              AND gt.go_term_id = gr.parent_term_id
              AND grt.go_relationship_type_id = gr.go_relationship_type_id
              AND grt.name = 'closure'
            group BY gt.go_id, gt.name
   ) bgd,
   (SELECT gt.go_id, count(distinct gts.source_id) as resultcnt
            FROM ApidbTuning.GoTermSummary gts,
                 sres.GoTerm gt,
                 sres.GoRelationship gr,
                 sres.GoRelationshipType grt,
                 ($geneResultSql) r
            WHERE gts.source_id = r.source_id
              AND gts.ontology = '$self->{subOntology}'
              AND gts.source in ($self->{sources})
              AND gts.evidence_code in ($self->{evidCodes})
              AND gts.is_not is null
              AND gr.child_term_id = gts.go_term_id
              AND gt.go_term_id = gr.parent_term_id
              AND gr.go_relationship_type_id = grt.go_relationship_type_id
              AND grt.name = 'closure'
            group BY gt.go_id
      ) rslt
where bgd.go_id = rslt.go_id
";
}

sub usage {
  my $this = basename($0);

  die "
Find pathways that are enriched in the provided set of Genes.

Usage: $this outputFile sqlToFindGeneList modelName pValueCutoff pathwaySources

Where:
  sqlToFindGeneList:    a select statement that will return all the rows in the db containing the genes result. Must have a source_id column.
  pValueCutoff:         the p-value exponent to use as a cutoff.  terms with a larger exponent are not returned
  outputFile:           the file in which to write results
  modelName:            eg, PlasmoDB.  Used to find the database connection.
  pathwaySources:       a list of pathway sources in format compatible with an sql in clause. only include pathways that comes from these one or more sources.  (Eg, KEGG).

The gene result must only include genes from a single taxon.  It is an error otherwise.

The output file is tab-delimited, with these columns (sorted by e-value)
  - e-value
  - GO ID
  - number of genes in organism with this term
  - number of genes in result with this term
  - GO TERM

";

}

1;
