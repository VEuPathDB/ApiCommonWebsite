package ApiCommonWebsite::View::GraphPackage::EuPathDB::Ferdig::Dd2Hb3Similarity;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;


use Data::Dumper;
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'grey');
  $self->setPlotWidth(450);

  my $secId = $self->getSecondaryId();
  my $jsonForService = "{\"profileSetName\":\"Profiles of DD2-HB3 expression from Ferdig\",\"profileType\":\"values\"},{\"profileSetName\":\"Profiles of DD2-HB3 expression from Ferdig\",\"profileType\":\"values\",\"idOverride\":\"$secId\"}";

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");

  my $similarity = EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot::LogRatio->new(@_);
  $similarity->setProfileSets([$profileSets]);
  $similarity->setColors(\@colors);
  $similarity->setLegendLabels(['Match', 'Query']);
  $similarity->setElementNameMarginSize(5);
  $similarity->setXaxisLabel("");

  my $rAdjustString = <<'RADJUST';
  profile.df.full$GROUP <- profile.df.full$LEGEND
RADJUST
  $similarity->setAdjustProfile($rAdjustString);

  $self->setGraphObjects($similarity);

  return $self;
}

1;

