package ApiCommonWebsite::View::GraphPackage::ToxoDB::Carruthers::IntraExtraDiff;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(6);

  my $colors = ['#D87093','#E9967A', '#87CEEB'];

  my $legend = ["Extracelluar", "Intracelluar(0hr)","Intracelluar(2hr)", ];

  $self->setMainLegend({colors => ['#D87093','#E9967A', '#87CEEB'], short_names => $legend, cols=> 3});

  my @profileSetsArray = (['Expression profiles of Tgondii ME49 Carruthers experiments', 'standard error - Expression profiles of Tgondii ME49 Carruthers experiments', '']);
  my @percentileSetsArray = (['percentile - Expression profiles of Tgondii ME49 Carruthers experiments', '',''],);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  $self->setMainLegend({colors => $colors, short_names => $legend, cols=> 2});


  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (9);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (9);

  $self->setGraphObjects($rma, $percentile);

  return $self;

}

1;
