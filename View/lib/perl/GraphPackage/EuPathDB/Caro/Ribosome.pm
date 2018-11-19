package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Caro::Ribosome;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24,15,17];
#  my $colors = ['#E57C24','#315B7D','#588EBB','#DDDDDD'];
  my $legend = ['Ribosome', 'mRNA - Sense', 'mRNA - Antisense','Translational Effeciency'];

  my $sampleLabels = ['R','ET', 'LT', 'S', 'M'];


  my $translationalEffSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Ribosome profile and mRNA transcriptome of asexual stages - translational efficiency - sense_strand', 'values', undef,undef, $sampleLabels,undef,undef,undef,'translational efficiency']]);

  my $id = $self->getId();

  my $transEff = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);
  $transEff->setProfileSets([$translationalEffSets->[0]]);
  $transEff->setYaxisLabel('Translational Effeciency');
  $transEff->setColors(["#a88b8b"]);
  $transEff->setElementNameMarginSize(6);
  $transEff->setPartName('trans_eff');
  $transEff->setSampleLabels($sampleLabels);
  $transEff->setPlotTitle("$id - Translational Efficiency");



  $self->setGraphObjects($transEff);

  return $self;

}

1;
