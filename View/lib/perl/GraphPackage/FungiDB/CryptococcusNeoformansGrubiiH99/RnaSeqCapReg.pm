package ApiCommonWebsite::View::GraphPackage::FungiDB::CryptococcusNeoformansGrubiiH99::RnaSeqCapReg;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::PercentilePlot;
use ApiCommonWebsite::View::GraphPackage::RNASeqStackedBarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  my $colors =['#29ACF2', '#DDDDDD'];
  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $sampleNames =["WT-30C", "WT-37C", "ada2-30C", "ada2-37C", "cir1-30C", "cir1-37C", "nrg1-30C", "nrg1-37C"];
 
  $self->setMainLegend({colors => $colors, short_names => $legend});
  my $stackedCoverage = ApiCommonWebsite::View::GraphPackage::RNASeqStackedBarPlot->new();
  $stackedCoverage->setProfileSetNames(['C.neoformans- ada2-delta, nrg1-delta, cir1-delta and KN99-alpha analysis', 
                                        'C.neoformans- ada2-delta, nrg1-delta, cir1-delta and KN99-alpha analysis-diff']);
  $stackedCoverage->setColors($colors);
  $stackedCoverage->setForceHorizontalXAxis(1);
  $stackedCoverage->setSampleLabels($sampleNames);
                                    
                   

  my $percentile = ApiCommonWebsite::View::GraphPackage::PercentilePlot->new();
  $percentile->setProfileSetNames(['percentile - C.neoformans- ada2-delta, nrg1-delta, cir1-delta and KN99-alpha analysis']);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setColors([$colors->[0]]);
  $percentile->setSampleLabels($sampleNames);
  $self->setGraphObjects($stackedCoverage, $percentile);
  return $self;
}



1;
