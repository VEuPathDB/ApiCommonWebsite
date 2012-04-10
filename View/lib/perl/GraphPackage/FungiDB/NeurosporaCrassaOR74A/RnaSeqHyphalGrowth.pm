package ApiCommonWebsite::View::GraphPackage::FungiDB::NeurosporaCrassaOR74A::RnaSeqHyphalGrowth;
use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#D87093', '#DDDDDD'];
  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  my $stackedCoverage = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new();
  $stackedCoverage->setProfileSetNames(['NcraOR74A Hyphal Growth RNASeq', 
                                        'NcraOR74A Hyphal Growth RNASeq - diff'
                                        ]);
  $stackedCoverage->setColors($colors);
  $stackedCoverage->setForceHorizontalXAxis(1);
  $stackedCoverage->setSampleLabels(['3 HR', 
                                     '5 HR', 
                                     '20 HR']);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new();
  $percentile->setProfileSetNames(['percentile - NcraOR74A Hyphal Growth RNASeq']);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setColors([$colors->[0]]);

  $self->setGraphObjects($stackedCoverage, $percentile);
  return $self;

}



1;
