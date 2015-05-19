package ApiCommonWebsite::Model::CannedQuery::ProfileByEC;
@ISA = qw( ApiCommonWebsite::Model::CannedQuery::Profile );

use strict;

use ApiCommonWebsite::Model::CannedQuery::Profile;

sub init {
  my $Self = shift;
  my $Args = ref $_[0] ? shift : {@_};

  $Self->SUPER::init($Args);

  $Self->setSql(<<Sql);
            select gene_source_id, ec.ec_number, p.profile_as_string
            from
            apidb.profileset ps, apidb.profile p,
            (SELECT DISTINCT gf.source_id gene_source_id, ec.ec_number
            FROM dots.GeneFeature gf, ApidbTuning.GenomicSequenceAttributes gs,
                 dots.Transcript t, dots.translatedAaFeature taf,
                 dots.aaSequenceEnzymeClass asec, sres.enzymeClass ec,ApidbTuning.GeneAttributes ga
            WHERE gs.na_sequence_id = gf.na_sequence_id
             AND ga.source_id = gf.source_id
             AND gf.na_feature_id = t.parent_id
             AND t.na_feature_id = taf.na_feature_id
             AND taf.aa_sequence_id = asec.aa_sequence_id
             AND asec.enzyme_class_id = ec.enzyme_class_id                                                                                                  
             AND ec.ec_number LIKE REPLACE(REPLACE(REPLACE(REPLACE(lower('<<Id>>'),' ',''),'-', '%'),'*','%'),'any','%')                       
            ) ec
            where ps.name = '<<ProfileSet>>'
            and ps.profile_set_id = p.profile_set_id
            and p.source_id = ec.gene_source_id
Sql

  return $Self;
}

1;
