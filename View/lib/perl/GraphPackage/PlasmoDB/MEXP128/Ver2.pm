package ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP128::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);

  my $colors = ['#A52A2A', '#B0C4DE','#483D8B'],
  my $xAxisLabels = ['ring', 'trophozoite', 'schizont'];

  $self->setMainLegend({colors => $colors, short_names => $xAxisLabels, cols=>3});

  my $loess = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new();
  $loess->setProfileSetNames(['Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4']);
  $loess->setStErrProfileSetNames(['standard error - Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4']);
  $loess->setColors($colors);
  $loess->setForceHorizontalXAxis(1);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new();
  $percentile->setProfileSetNames(['red percentile - Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4',
                                   'green percentile - Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4']);
  $percentile->setColors(['#A52A2A', '#FFDAB9', '#B0C4DE','#FFDAB9','#483D8B','#FFDAB9']);
  $percentile->setForceHorizontalXAxis(1);
  $self->setGraphObjects($loess, $percentile);

  return $self;


}

1;



