package ApiCommonWebsite::Model::CannedQuery::ProfileByEC;
@ISA = qw( ApiCommonWebsite::Model::CannedQuery::Profile );

use strict;

use ApiCommonWebsite::Model::CannedQuery::Profile;

sub init {
  my $Self = shift;
  my $Args = ref $_[0] ? shift : {@_};

  $Self->SUPER::init($Args);

  $Self->setSql(<<Sql);
            select p.source_id, ec.ec_number, p.profile_as_string
            from apidbtuning.profile p,
            (SELECT DISTINCT ta.gene_source_id, ec.ec_number
            FROM  dots.aaSequenceEnzymeClass asec, sres.enzymeClass ec,ApidbTuning.TranscriptAttributes ta
            WHERE ta.aa_sequence_id = asec.aa_sequence_id
             AND asec.enzyme_class_id = ec.enzyme_class_id                                                                                                  
             AND ec.ec_number LIKE REPLACE(REPLACE(REPLACE(REPLACE(lower('<<Id>>'),' ',''),'-', '%'),'*','%'),'any','%')                       
            ) ec
   WHERE  p.profile_set_name          = '<<ProfileSet>>'
    AND p.profile_type           = '<<ProfileType>>'
    AND p.gene_source_id = ec.gene_source_id




Sql

  return $Self;
}

1;
