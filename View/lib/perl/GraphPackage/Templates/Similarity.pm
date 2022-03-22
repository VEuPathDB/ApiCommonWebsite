package ApiCommonWebsite::View::GraphPackage::Templates::Similarity;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;
#use Data::Dumper;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my @colors = ('blue', 'grey');

  my $profile = $self->getProfile;
  my $secId = $self->getSecondaryId();
  my $jsonForService = "{\"profileSetName\":\"$profile\",\"profileType\":\"values\"},{\"profileSetName\":\"$profile\",\"profileType\":\"values\",\"idOverride\":\"$secId\"}";

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");

  my $similarity = EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot::LogRatio->new(@_);
  $similarity->setProfileSets([$profileSets]);
  $similarity->setColors(\@colors);
  $similarity->setLegendLabels(['Match', 'Query']);
  $similarity->setElementNameMarginSize(6.5);
  $similarity->setXaxisLabel($self->getXAxisLabel());
  $self->setGraphObjects($similarity);

  my $rAdjustString = <<'RADJUST';
  profile.df.full$GROUP <- profile.df.full$LEGEND
RADJUST
  $similarity->setAdjustProfile($rAdjustString);

  return $similarity;
}


sub getProfile {
 return ;
}

1;

# TEMPLATE_ANCHOR similarityGraph


#cneoH99_Haase_Kelliher_Cell_Cycle_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Similarity::DS_6393f11883;
sub getProfile {
  my ($self) = @_;
  my $pset = 'C. neoformans cell-cycle RNAseq [htseq-union - firststrand - tpm - unique]';

  return $pset;
}

1;

#scerS288c_Haase_Kelliher_Cell_Cycle_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Similarity::DS_1a53359a85;
sub getProfile {
  my ($self) = @_;
  my $pset = 'S. cerevisiae cell-cycle RNAseq [htseq-union - firststrand - tpm - unique]';

  return $pset;
}

1;


# scerS288c_microarrayExpression_Spellman_CellCycle_1998_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Similarity::DS_f101fb2669;
sub getProfile {
  my ($self) = @_;
  my $pset = 'S. cerevisiae cell-cycle RNAseq [htseq-union - firststrand - fpkm - unique]';

  return $pset;
}
1;

# ncraOR74A_Clock_Regulated_Genes_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Similarity::DS_7835aa4647;
sub getProfile {
  my ($self) = @_;
  my $pset = 'N. crassa analysis of clock-regulated genes [htseq-union - firststrand - fpkm - unique]';

  return $pset;
}
1;


# HostDB mmusC57BL6J_Saeij_Jeroen_strains_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Similarity::DS_b8755b3393;

sub getProfile {
  my ($self) = @_;
  my $pset = 'Murine macrophages infected by 29 different strains of T. gondii [htseq-union - unstranded - tpm - unique]';

  return $pset;
}
1;

