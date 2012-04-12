package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Cowman::Sir2KO;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#6495ED', '#E9967A', '#2F4F4F' ];
  my $legend = ['Wild Type', 'sir2A', 'sir2B'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});

  my $radjust = "colnames(profile.df) = rep(c(\"ring\", \"trophozoite\", \"schizont\"), 3);profile.df = rbind(profile.df[1,1:3], profile.df[1,4:6], profile.df[1,7:9]);";

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new();
  $rma->setProfileSetNames(['Profiles of E-TABM-438 from Cowman']);
  $rma->setStErrProfileSetNames(['standard error - Profiles of E-TABM-438 from Cowman']);
  $rma->setAdjustProfile($radjust . "colnames(stdev.df) = rep(c(\"ring\", \"trophozoite\", \"schizont\"), 3);stdev.df = rbind(stdev.df[1,1:3], stdev.df[1,4:6], stdev.df[1,7:9]);");
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new();
  $percentile->setProfileSetNames(['percentile - Profiles of E-TABM-438 from Cowman']);
  $percentile->setAdjustProfile($radjust);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;










