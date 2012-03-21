package ApiCommonWebsite::View::GraphPackage::FungiDB::CryptococcusNeoformansGrubiiH99::RnaSeqNrg1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::PercentilePlot;
use ApiCommonWebsite::View::GraphPackage::RNASeqStackedBarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  my $colors =['#D87093', '#DDDDDD'];
  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];

  $self->setMainLegend({colors => $colors, short_names => $legend});
  my $stackedCoverage = ApiCommonWebsite::View::GraphPackage::RNASeqStackedBarPlot->new();
  $stackedCoverage->setProfileSetNames(['C.neoformans NRG1 Expression', 
                                        'C.neoformans NRG1 Expression-diff']);
  $stackedCoverage->setColors($colors);
  $stackedCoverage->setForceHorizontalXAxis(1);
  $stackedCoverage->setSampleLabels(['H99 Wildtype','nrg1 KO',
                                     'nrg1 Over-expression']);

  my $percentile = ApiCommonWebsite::View::GraphPackage::PercentilePlot->new();
  $percentile->setProfileSetNames(['percentile - C.neoformans NRG1 Expression']);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setColors([$colors->[0]]);
  
  $self->setGraphObjects($stackedCoverage, $percentile);
  return $self;
}



1;
