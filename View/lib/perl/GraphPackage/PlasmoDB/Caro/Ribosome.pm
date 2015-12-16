package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Caro::Ribosome;

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
  my $colors = ['#E57C24', '#FFB87E','#315B7D','#588EBB','#DDDDDD'];
  my $legend = ['Ribosome - Sense', 'Ribosome - Antisense', 'Steady State - Sense', 'Steady State - Antisense'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 2});
  
  my @profileArray = (
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - antisense strand'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - antisense strand - diff'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand - diff'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand - diff'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand - diff'],
                     );


  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand'],['percentile - Ribosome profile and mRNA transcriptome of asexual stages - ribosome - antisense strand'],['percentile - Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand'],['percentile - Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand']]);
  
  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0],$profileSets->[2],$profileSets->[4],$profileSets->[6],]);
  $line->setPartName('rpkm_line');
  $line->setAdjustProfile('lines.df=lines.df + 1; lines.df = log2(lines.df);');
  $line->setYaxisLabel('RPKM (log2)');
  $line->setPointsPch($pch);
  $line->setColors([$colors->[0], $colors->[1],$colors->[2], $colors->[3],]);
  $line->setArePointsLast(1);
  $line->setElementNameMarginSize(6);
  $line->setXaxisLabel('Hours post infection');
  $line->setPartName('rpkm_time_course');
  my $id = $self->getId();
  $line->setPlotTitle("RPKM - $id - Time Course");


  my $partName;
  my $stackedRibosomeSense = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stackedRibosomeSense->setProfileSets([$profileSets->[2], $profileSets->[3]]);
  $stackedRibosomeSense->setColors([$colors->[0],$colors->[4]] );
  $stackedRibosomeSense->setPartName('rpkm_ribosome_sense');
  my $basePlotTitle = $stackedRibosomeSense->getPlotTitle;
  $stackedRibosomeSense->setPlotTitle($basePlotTitle." - sense - Ribosome");
  $stackedRibosomeSense->setElementNameMarginSize(6);


  my $stackedRibosomeAnti = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stackedRibosomeAnti->setProfileSets([$profileSets->[0], $profileSets->[1]]);
  $stackedRibosomeAnti->setColors([$colors->[1],$colors->[4]] );
  $stackedRibosomeAnti->setPartName('rpkm_ribosome_anti');
  my $basePlotTitle = $stackedRibosomeAnti->getPlotTitle;
  $stackedRibosomeAnti->setPlotTitle($basePlotTitle." - antisense - Ribosome");
  $stackedRibosomeAnti->setElementNameMarginSize(6);


  my $stackedSteadySense = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stackedSteadySense->setProfileSets([$profileSets->[4], $profileSets->[5]]);
  $stackedSteadySense->setColors([$colors->[2],$colors->[4]] );
  $stackedSteadySense->setPartName('rpkm_steady_sense');
  my $basePlotTitle = $stackedSteadySense->getPlotTitle;
  $stackedSteadySense->setPlotTitle($basePlotTitle." - sense - Steady State");
  $stackedSteadySense->setElementNameMarginSize(6);

  my $stackedSteadyAnti = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stackedSteadyAnti->setProfileSets([$profileSets->[6], $profileSets->[7]]);
  $stackedSteadyAnti->setColors([$colors->[3],$colors->[4]] );
  $stackedSteadyAnti->setPartName('rpkm_steady_anti');
  my $basePlotTitle = $stackedSteadyAnti->getPlotTitle;
  $stackedSteadyAnti->setPlotTitle($basePlotTitle." - antisense - Steady State");
  $stackedSteadyAnti->setElementNameMarginSize(6);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets([$percentileSets->[0],$percentileSets->[2],]);
  $percentile->setColors([$colors->[0],$colors->[2],]);
  $percentile->setArePointsLast(1);
  $percentile->setPartName('percentile_sense');
  $percentile->setXaxisLabel('Hours post infection');
  $percentile->setPlotTitle($basePlotTitle." - percentile - sense");
  $percentile->setElementNameMarginSize(6);

  $self->setGraphObjects($line, $stackedRibosomeSense, $stackedRibosomeAnti,$stackedSteadySense,$stackedSteadyAnti, $percentile,);

  return $self;
}

1;
