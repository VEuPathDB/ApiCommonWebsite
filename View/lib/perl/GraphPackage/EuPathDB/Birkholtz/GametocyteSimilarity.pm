package ApiCommonWebsite::View::GraphPackage::EuPathDB::Birkholtz::GametocyteSimilarity;

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
  my @sampleNames = ('2-day pre-induction','1-day pre-induction','0-day pre-induction','1-day post induction','2-day post induction','3-day post induction','4-day post induction','5-day post induction','6-day post induction','7-day post induction','8-day post induction','9-day post induction','10-day post induction','11-day post induction','12-day post induction','13-day post induction');
  $self->setPlotWidth(450);

  my $secId = $self->getSecondaryId();
  my $jsonForService = "{\"profileSetName\":\"Pfal3D7 Gametocyte time course\",\"profileType\":\"values\"},{\"profileSetName\":\"Pfal3D7 Gametocyte time course\",\"profileType\":\"values\",\"idOverride\":\"$secId\"}";

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");  

  my $similarity = EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot::LogRatio->new(@_);
  $similarity->setProfileSets([$profileSets]);
  $similarity->setColors(\@colors);
  $similarity->setLegendLabels(['Match', 'Query']);
  $similarity->setElementNameMarginSize(5);
  $similarity->setSampleLabels(\@sampleNames);
  $similarity->setXaxisLabel("");

  my $adjust = "
profile.is.numeric <- FALSE
profile.df.full\$ELEMENT_NAMES <- factor(profile.df.full\$ELEMENT_NAMES, levels = c('2-day pre-induction','1-day pre-induction','0-day pre-induction','1-day post induction','2-day post induction','3-day post induction','4-day post induction','5-day post induction','6-day post induction','7-day post induction','8-day post induction','9-day post induction','10-day post induction','11-day post induction','12-day post induction','13-day post induction'))
profile.df.full\$GROUP <- profile.df.full\$LEGEND";

  $similarity->addAdjustProfile($adjust);

  $self->setGraphObjects($similarity);

  return $self;
}

1;

