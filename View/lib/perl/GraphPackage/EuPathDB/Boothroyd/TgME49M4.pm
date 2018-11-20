package ApiCommonWebsite::View::GraphPackage::EuPathDB::Boothroyd::TgME49M4;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#D87093', '#D87093', '#D87093', '#E9967A', '#87CEEB', '#87CEEB', '#87CEEB'];

  my $legend = ["oocyst", "tachyzoite", "bradyzoite"];

  $self->setMainLegend({colors => ['#D87093', '#E9967A', '#87CEEB'], short_names => $legend, cols=> 3});


   my @profileSetsArray = (['Expression profiles of Tgondii ME49 Boothroyd experiments', 'standard error - Expression profiles of Tgondii ME49 Boothroyd experiments', '']);
  my @percentileSetsArray = (['percentile - Expression profiles of Tgondii ME49 Boothroyd experiments', '',''],);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (10);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (10);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}

1;
