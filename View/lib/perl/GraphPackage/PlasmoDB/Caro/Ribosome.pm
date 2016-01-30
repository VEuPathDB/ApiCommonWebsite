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
  my $colors = ['#E57C24','#315B7D','#588EBB','#DDDDDD'];
  my $legend = ['Ribosome', 'mRNA - Sense', 'mRNA - Antisense','Translational Effeciency'];

  my $sampleLabels = ['R','ET', 'LT', 'S', 'M'];
  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});
  
  my @profileArray = (
                      ['Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand', undef, $sampleLabels,undef,undef,undef,'RPKM - ribosome'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand', undef, $sampleLabels,undef,undef,undef,'RPKM - mRNA sense'],
                      ['Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand', undef, $sampleLabels,undef,undef,undef,'RPKM - mRNA antisense'],

                     );


  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - Ribosome profile and mRNA transcriptome of asexual stages - ribosome - sense strand',undef, $sampleLabels,undef,undef,undef,'percentile - ribosome'],
                                                                                    ['percentile - Ribosome profile and mRNA transcriptome of asexual stages - steady_state - sense strand',undef, $sampleLabels,undef,undef,undef,'percentile - mRNA sense'],
                                                                                    ['percentile - Ribosome profile and mRNA transcriptome of asexual stages - steady_state - antisense strand',undef, $sampleLabels,undef,undef,undef,'mRNA antisense']]);

  my $translationalEffSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['Ribosome profile and mRNA transcriptome of asexual stages - translational efficiency - sense_strand',undef, $sampleLabels,undef,undef,undef,'translational efficiency']]);

  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0],$profileSets->[1],$profileSets->[2]]);
  $line->setYaxisLabel('RPKM');
  $line->setPointsPch($pch);
  $line->setColors([$colors->[0],$colors->[1], $colors->[2],]);
  $line->setArePointsLast(1);
  $line->setElementNameMarginSize(6);
  $line->setXaxisLabel('Life Cycle Stage');
  $line->setPartName('rpkm');
  $line->setSampleLabels($sampleLabels);
  my $id = $self->getId();
  $line->setPlotTitle("RPKM - $id - Time Course");
  $line->setForceConnectPoints(1);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets([$percentileSets->[0],$percentileSets->[1],$percentileSets->[2]]);
  $percentile->setColors([$colors->[0],$colors->[1],$colors->[2],]);
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
  $transEff->setYaxisLabel('Translational Effeciency');
  $transEff->setColors([$colors->[3]]);
  $transEff->setElementNameMarginSize(6);
  $transEff->setPartName('trans_eff');
  $transEff->setSampleLabels($sampleLabels);
  $transEff->setPlotTitle("$id - Translational Efficiency");
  $self->setGraphObjects($transEff,$line, $percentile);

  return $self;

}

1;
