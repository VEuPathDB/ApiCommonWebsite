package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Newbold::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(480);

  my $colors = ['lightskyblue2', 'red2'];
  my $xAxisLabels = ['mild disease', 'severe disease'];

  $self->setMainLegend({colors => $colors, short_names => $xAxisLabels, cols=>2});

  my @allColors;
  foreach(1..8) {
    push @allColors, $colors->[0];
  }
  foreach(1..9) {
    push @allColors, $colors->[1];
  }

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new();
  $rma->setProfileSetNames(['newbold gene profiles sorted mild-severe']);
  $rma->setColors(\@allColors);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new();
  $percentile->setProfileSetNames(['percentile - newbold gene profiles sorted mild-severe']);
  $percentile->setColors(\@allColors);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;
