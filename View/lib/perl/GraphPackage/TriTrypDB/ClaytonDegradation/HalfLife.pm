package ApiCommonWebsite::View::GraphPackage::TriTrypDB::ClaytonDegradation::HalfLife;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24,15,17];
  my $colors =['#F2BD79','#996622','#99C5FF','#0049A8','#DDDDDD'];
  my $legend = ['Procyclic Form', 'Procyclic Form - Scaled', 'Bloodstream Form','Bloodstream Form - Scaled'];

  my $sampleLabels = [];
#  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});
  
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(
                      [
                       ['mRNA decay in bloodstream and procyclic form - PC', undef, $sampleLabels,undef,undef,undef,'procyclic form'],
                       ['mRNA decay in bloodstream and procyclic form - PC_scaled', undef, $sampleLabels,undef,undef,undef,'procyclic form - scaled'],
                       ['mRNA decay in bloodstream and procyclic form - BF', undef, $sampleLabels,undef,undef,undef,'bloodstream form'],
                       ['mRNA decay in bloodstream and procyclic form - BF_scaled', undef, $sampleLabels,undef,undef,undef,'bloodstream form - scaled'],
                      ]);

  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([
                                                                                    ['percentile - mRNA decay in bloodstream and procyclic form - PC',undef, $sampleLabels,undef,undef,undef,'percentile - mRNA sense'],
                                                                                    ['percentile - mRNA decay in bloodstream and procyclic form - BF',undef, $sampleLabels,undef,undef,undef,'mRNA antisense']]);

  my $halfLifeSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['mRNA decay in bloodstream and procyclic form - half_life','mRNA decay in bloodstream and procyclic form - half_life_error', $sampleLabels,undef,undef,undef,'half-life']]);

  my $legendLabels = (['PC','PC Scaled','BF','BF Scaled']);

  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets($profileSets);
  $line->setColors([$colors->[0],$colors->[1], $colors->[2], $colors->[3]]);
  $line->setYaxisLabel('RPKM');
  #$line->setPointsPch($pch);
  $line->setPartName('rpkm');
  $line->setXaxisLabel('Life Cycle Stage');
  my $id = $self->getId();
  $line->setPlotTitle("RPKM - $id");
  $line->setSampleLabels($sampleLabels);
  $line->setElementNameMarginSize(4);
  $line->setIsLogged(0);
  $line->setHasExtraLegend(1);
  $line->setLegendLabels($legendLabels);
  $line->setExtraLegendSize(6);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors->[0],$colors->[2],]);
  #$percentile->setArePointsLast(1);
  $percentile->setPartName('percentile');
  $percentile->setXaxisLabel('Life Cycle Stage');
  my $basePlotTitle = $percentile->getPlotTitle;
  $percentile->setPlotTitle($basePlotTitle." - percentile");
  $percentile->setSampleLabels($sampleLabels);
  $percentile->setElementNameMarginSize(4);
  $percentile->setHasExtraLegend(1);
  $percentile->setLegendLabels([$legendLabels->[0],$legendLabels->[2]]);
  $percentile->setExtraLegendSize(6);

  my $halfLife = ApiCommonWebsite::View::GraphPackage::BarPlot->new(@_);
  $halfLife->setProfileSets([$halfLifeSets->[0]]);
  $halfLife->setYaxisLabel('hours');
  $halfLife->setColors(['#DDDDDD']);
  $halfLife->setElementNameMarginSize(4);
  $halfLife->setPartName('half-life');
  $halfLife->setSampleLabels($sampleLabels);
  $halfLife->setPlotTitle("$id - HalfLife");
  $self->setGraphObjects($halfLife,$line, $percentile);

  return $self;

}

1;
