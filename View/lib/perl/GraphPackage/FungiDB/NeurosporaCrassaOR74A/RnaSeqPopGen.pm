package ApiCommonWebsite::View::GraphPackage::FungiDB::NeurosporaCrassaOR74A::RnaSeqPopGen;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  my $colors =['#29ACF2', '#DDDDDD'];
  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $sampleNames =["FGSC_1131", "FGSC_1132", "FGSC_1133", "FGSC_1165", "FGSC_2229b", "FGSC_2229", "FGSC_3199", "FGSC_3200", "FGSC_3211", "FGSC_3223", "FGSC_3943", "FGSC_3968", "FGSC_3975", "FGSC_4708", "FGSC_4712", "FGSC_4713", "FGSC_4715", "FGSC_4716", "FGSC_4730", "FGSC_4824", "FGSC_5910", "FGSC_5914", "FGSC_6203", "FGSC_851", "FGSC_8783", "FGSC_8784", "FGSC_8787", "FGSC_8789", "FGSC_8790", "FGSC_8816", "FGSC_8819", "FGSC_8829", "FGSC_8845", "FGSC_8848", "FGSC_8850", "FGSC_8851", "FGSC_8870", "FGSC_8871", "FGSC_8872", "FGSC_8874", "FGSC_8876", "FGSC_8878", "Perkins_4450", "Perkins_4452", "Perkins_4455", "Perkins_4465", "Perkins_4476", "Perkins_4496"];
 
  $self->setMainLegend({colors => $colors, short_names => $legend});
  my $stackedCoverage = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new();
  $stackedCoverage->setProfileSetNames(['N Crassa population genomics', 
                                        'N Crassa population genomics-diff']);
  $stackedCoverage->setColors($colors);
#  $stackedCoverage->setForceHorizontalXAxis(1);
  $stackedCoverage->setSampleLabels($sampleNames);
  $stackedCoverage->setBottomMarginSize(5);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new();
  $percentile->setProfileSetNames(['percentile - N Crassa population genomics']);
#  $percentile->setForceHorizontalXAxis(1);
  $percentile->setColors([$colors->[0]]);
  $percentile->setSampleLabels($sampleNames);
  $percentile->setBottomMarginSize(5);
  $self->setGraphObjects($stackedCoverage, $percentile);
  return $self;
}



1;
