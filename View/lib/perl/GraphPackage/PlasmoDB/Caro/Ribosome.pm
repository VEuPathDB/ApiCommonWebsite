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
  my $legend = ['Ribosome - Sense', 'Ribosome - Antisense', 'Steady State - Sense', 'Steady State - Antisense','Translational Effeciency'];

  my $sampleLabels = ['R','ET', 'LT', 'S', 'M'];
  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});
  
  my @profileArray = (
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand', undef, $sampleLabels],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand - diff', undef, $sampleLabels],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - antisense strand', undef, $sampleLabels],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - antisense strand - diff', undef, $sampleLabels],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand', undef, $sampleLabels],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand - diff', undef, $sampleLabels],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand', undef, $sampleLabels],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand - diff', undef, $sampleLabels],
                     );


  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand',undef, $sampleLabels],
                                                                                    ['percentile - Ribosome profile and mRNA transcriptome of asexual stages - ribosome - antisense strand',undef, $sampleLabels],
                                                                                    ['percentile - Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand',undef, $sampleLabels],
                                                                                    ['percentile - Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand',undef, $sampleLabels]]);

  my $translationalEffSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['Ribosome profile and mRNA transcriptome of asexual stages - translational efficiency - sense_strand',undef, $sampleLabels],
['Ribosome profile and mRNA transcriptome of asexual stages - translational efficiency - antisense_strand',undef, $sampleLabels],]);
  
  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0],$profileSets->[2],$profileSets->[4],$profileSets->[6],]);
  $line->setYaxisLabel('RPKM');
  $line->setPointsPch($pch);
  $line->setColors([$colors->[0], $colors->[1],$colors->[2], $colors->[3],]);
  $line->setArePointsLast(1);
  $line->setElementNameMarginSize(6);
  $line->setXaxisLabel('Life Cycle Stage');
  $line->setPartName('rpkm');
  $line->setSampleLabels($sampleLabels);
  my $id = $self->getId();
  $line->setPlotTitle("RPKM - $id - Time Course");
  $line->setForceConnectPoints(1);


# Removed Stack Bar plots based on request from Outreach Team.

    # my $partName;
    # my $stackedRibosomeSense = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
    # $stackedRibosomeSense->setProfileSets([$profileSets->[0], $profileSets->[1]]);
    # $stackedRibosomeSense->setColors([$colors->[0],$colors->[4]] );
    # $stackedRibosomeSense->setPartName('rpkm_ribosome_sense');
    # my $basePlotTitle = $stackedRibosomeSense->getPlotTitle;
    # $stackedRibosomeSense->setPlotTitle($basePlotTitle." - sense - Ribosome");
    # $stackedRibosomeSense->setSampleLabels($sampleLabels);
    # $stackedRibosomeSense->setElementNameMarginSize(6);


    # my $stackedRibosomeAnti = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
    # $stackedRibosomeAnti->setProfileSets([$profileSets->[2], $profileSets->[3]]);
    # $stackedRibosomeAnti->setColors([$colors->[1],$colors->[4]] );
    # $stackedRibosomeAnti->setPartName('rpkm_ribosome_anti');
    # my $basePlotTitle = $stackedRibosomeAnti->getPlotTitle;
    # $stackedRibosomeAnti->setPlotTitle($basePlotTitle." - antisense - Ribosome");
    # $stackedRibosomeAnti->setSampleLabels($sampleLabels);
    # $stackedRibosomeAnti->setElementNameMarginSize(6);


    # my $stackedSteadySense = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
    # $stackedSteadySense->setProfileSets([$profileSets->[4], $profileSets->[5]]);
    # $stackedSteadySense->setColors([$colors->[2],$colors->[4]] );
    # $stackedSteadySense->setPartName('rpkm_steady_sense');
    # my $basePlotTitle = $stackedSteadySense->getPlotTitle;
    # $stackedSteadySense->setPlotTitle($basePlotTitle." - sense - Steady State");
    # $stackedSteadySense->setSampleLabels($sampleLabels);
    # $stackedSteadySense->setElementNameMarginSize(6);

    # my $stackedSteadyAnti = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
    # $stackedSteadyAnti->setProfileSets([$profileSets->[6], $profileSets->[7]]);
    # $stackedSteadyAnti->setColors([$colors->[3],$colors->[4]] );
    # $stackedSteadyAnti->setPartName('rpkm_steady_anti');
    # my $basePlotTitle = $stackedSteadyAnti->getPlotTitle;
    # $stackedSteadyAnti->setPlotTitle($basePlotTitle." - antisense - Steady State");
    # $stackedSteadyAnti->setSampleLabels($sampleLabels);
    # $stackedSteadyAnti->setElementNameMarginSize(6);


  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets([$percentileSets->[0],$percentileSets->[1],$percentileSets->[2],$percentileSets->[3],]);
  $percentile->setColors([$colors->[0],$colors->[1],$colors->[2],$colors->[3],]);
  $percentile->setArePointsLast(1);
  $percentile->setPartName('percentile');
  $percentile->setXaxisLabel('Life Cycle Stage');
  my $basePlotTitle = $percentile->getPlotTitle;
  $percentile->setPlotTitle($basePlotTitle." - percentile");
  $percentile->setSampleLabels($sampleLabels);
  $percentile->setElementNameMarginSize(6);
  $percentile->setForceConnectPoints(1);
  #$self->setGraphObjects($line, $stackedRibosomeSense, $stackedRibosomeAnti,$stackedSteadySense,$stackedSteadyAnti, $percentile,);

  my $transEff = ApiCommonWebsite::View::GraphPackage::BarPlot->new(@_);
  $transEff->setProfileSets([$translationalEffSets->[0]]);
  $transEff->setYaxisLabel('Efficiency Ratio');
  $transEff->setColors([$colors->[4]]);
  $transEff->setElementNameMarginSize(6);
  $transEff->setPartName('trans_eff');
  $transEff->setSampleLabels($sampleLabels);
  my $id = $self->getId();
  $transEff->setPlotTitle("$id - Translational Efficiency");
  $self->setGraphObjects($line, $percentile,$transEff);

  return $self;

}

1;
