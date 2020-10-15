package ApiCommonWebsite::View::GraphPackage::EuPathDB::White::CellCycleSimilarity;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'grey');
  $self->setPlotWidth(450);

  my $secId = $self->getSecondaryId();
  my $jsonForService = "{\"profileSetName\":\"M.White Cell Cycle Microarray\",\"profileType\":\"values\"},{\"profileSetName\":\"M.White Cell Cycle Microarray\",\"profileType\":\"values\",\"idOverride\":\"$secId\"}";

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");

  my $similarity = EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot::LogRatio->new(@_);
  $similarity->setProfileSets([$profileSets]);
  $similarity->setColors(\@colors);
  $similarity->setLegendLabels(['Match', 'Query']);
  $similarity->setElementNameMarginSize(6.5);
  $similarity->setXaxisLabel("Time Point (hours)");

  my $rAdjustString = <<'RADJUST';
  profile.df.full$GROUP <- profile.df.full$LEGEND
RADJUST
  $similarity->setAdjustProfile($rAdjustString);

  $self->setGraphObjects($similarity);

  return $self;
}

1;

