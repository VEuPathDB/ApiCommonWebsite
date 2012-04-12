package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Su::CQTreatment;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#F08080', '#7CFC00' ];
  my $legend = ['untreated', 'chloroquine'];
  my $pch = [22];
  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  my $radjust = "colnames(profile.df) = c(rep(\"106/1\", 2), rep(\"106/1(76I)\", 2), rep(\"106/1(76I_352K)\",2));profile.df = rbind(profile.df[1, c(1,3,5)],profile.df[1,c(2,4,6)]);";

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new();
  $rma->setProfileSetNames(['E-GEOD-10022 array from Su']);
  $rma->setStErrProfileSetNames(['standard error - E-GEOD-10022 array from Su']);
  $rma->setAdjustProfile($radjust . "colnames(stdev.df) = c(rep(\"106/1\", 2), rep(\"106/1(76I)\", 2), rep(\"106/1(76I_352K)\",2));stdev.df = rbind(stdev.df[1, c(1,3,5)],stdev.df[1,c(2,4,6)]);");
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new();
  $percentile->setProfileSetNames(['percentile - E-GEOD-10022 array from Su']);
  $percentile->setAdjustProfile($radjust);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;
