package ApiCommonWebsite::View::GraphPackage::ToxoDB::Sullivan::GCN5A;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(300);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(12);

  my $colors = ['#D87093', '#D87093','#87CEEB', '#87CEEB'];
  my $legendColors = [@$colors[1..2]];

  my $legend = ["Wild Type", "GCN5-A Knockout", ];

  my @profileSetsArray = (['Toxoplasma gondii GCN5-A Knockout Array from Sullivan', 'standard error - Toxoplasma gondii GCN5-A Knockout Array from Sullivan', '']);
  my @percentileSetsArray = (['percentile - Toxoplasma gondii GCN5-A Knockout Array from Sullivan', '',''],);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  $self->setMainLegend({colors => $legendColors, short_names => $legend, cols=> 2});


  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (12);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (12);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}


1;
