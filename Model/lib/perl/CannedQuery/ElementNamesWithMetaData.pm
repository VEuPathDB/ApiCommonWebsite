
package ApiCommonWebsite::Model::CannedQuery::ElementNamesWithMetaData;
@ISA = qw( ApiCommonWebsite::Model::CannedQuery::ElementNames);

=pod

=head1 Purpose

This canned query selects the element names and associated metadata for a profileSet with a
given name and a given meta data category.

=head1 Macros

The following macros must be available to execute this query.

=over

=item ProfileSet - source id of the profile set. Category - Ontology entry value (metadata parent term).

=back

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use FileHandle;

use ApiCommonWebsite::Model::CannedQuery;

use ApiCommonWebsite::Model::CannedQuery::ElementNames;

use Data::Dumper;
# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;
  my $Args = ref $_[0] ? shift : {@_};

  $Self->SUPER::init($Args);

  $Self->setMetaDataCategory           ( $Args->{MetaDataCategory          } );

  $Self->setSql(<<Sql);
SELECT distinct pen.element_order, Pen.name || ':' || BMC.Value As Name       
FROM   study.Study                     s
,      apidb.ProfileSet                ps
,      apidb.ProfilEelementName        pen
,      rad.StudyBioMaterial            sbm
,      study.BioSample                 bs
,      study.BioMaterialCharacteristic bmc
,      study.OntologyEntry oe
WHERE ps.external_database_release_id = s.external_database_release_id
AND s.study_id = sbm.study_id 
AND bs.name = pen.name
AND pen.profile_set_id = ps.profile_set_id
AND bs.bio_material_id = sbm.bio_material_id
AND bs.bio_material_id = bmc.bio_material_id 
AND bmc.ontology_entry_id = oe.ontology_entry_id
AND ps.name = '<<ProfileSet>>'
AND lower(oe.value) = lower('<<MetaDataCategory>>')
ORDER BY pen.element_order
Sql

  return $Self;
}

# -------------------------------- access --------------------------------

sub getMetaDataCategory           { $_[0]->{'MetaDataCategory'        } }
sub setMetaDataCategory           { $_[0]->{'MetaDataCategory'        } = $_[1]; $_[0] }

# ========================================================================
# --------------------------- Support Methods ----------------------------
# ========================================================================

sub prepareDictionary {
	 my $Self = shift;
	 my $Dict = shift || {};

         $Dict->{ProfileSet} = $Self->getProfileSet();
	 $Dict->{MetaDataCategory} = $Self->getMetaDataCategory();

         my $Rv = $Dict;

	 return $Rv;
}



# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;




