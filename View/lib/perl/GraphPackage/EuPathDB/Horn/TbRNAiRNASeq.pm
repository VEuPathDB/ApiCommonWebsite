package ApiCommonWebsite::View::GraphPackage::EuPathDB::Horn::TbRNAiRNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;

use EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq;
use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors0 =['#191970', '#DDDDDD'];
  my $colors1 =['#B8860B', '#DDDDDD'];

  my $legendColors = [$colors0->[0], @$colors1];
  my $legend = ["Uniquely Mapped - CDS", "Uniquely Mapped - Transcript", "Non-Uniquely Mapped"];

  my $xAxisLabels = ['No_Tet',
                     'BFD3',
                     'BFD6',
                     'PF',
                     'DIF'];

  $self->setMainLegend({colors => $legendColors, short_names => $legend});


  my $transcript = EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq->new(@_);



  $transcript->setMinRpkmProfileSet('T.brucei paired end RNA Seq data from Horn');
  $transcript->setDiffRpkmProfileSet('T.brucei paired end RNA Seq data from Horn - diff');
  $transcript->setPctProfileSet('percentile - T.brucei paired end RNA Seq data from Horn');
  $transcript->setColor($colors0->[0]);
  $transcript->setIsPairedEnd(1);
  $transcript->makeGraphs(@_);
  $transcript->setForceXLabelsHorizontalString(1);
  $transcript->setBottomMarginSize(6);
  $transcript->setAdditionalRCode('profile=profile + 1; profile = log2(profile);');
#  $transcript->setSampleNames($self->getSampleNames);

  my ($transcriptStacked, $transcriptPct) = @{$transcript->getGraphObjects()};
  $transcriptStacked->setPartName("transcript_" . $transcriptStacked->getPartName);
  $transcriptPct->setPartName("transcript_" . $transcriptPct->getPartName);
  $transcriptStacked->setPlotTitle($transcriptStacked->getPlotTitle() . " - full gene model");
  $transcriptPct->setPlotTitle($transcriptPct->getPlotTitle() . " - full gene model");

  my $cds = EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq->new(@_);

  $cds->setMinRpkmProfileSet('T.brucei paired end RNA Seq data from Horn aligned with cds coordinates');
  $cds->setDiffRpkmProfileSet('T.brucei paired end RNA Seq data from Horn aligned with cds coordinates - diff');
  $cds->setPctProfileSet('percentile - T.brucei paired end RNA Seq data from Horn aligned with cds coordinates');
  $cds->setColor($colors1->[0]);
  $cds->setIsPairedEnd(1);
  $cds->makeGraphs(@_);
  $cds->setBottomMarginSize(6);
  $cds->setAdditionalRCode('profile=profile + 1; profile = log2(profile);');
#  $cds->setSampleNames($self->getSampleNames);
  $cds->setForceXLabelsHorizontalString(1);

  my ($cdsStacked, $cdsPct) = @{$cds->getGraphObjects()};
  $cdsStacked->setPartName("cds_" . $cdsStacked->getPartName);
  $cdsPct->setPartName("cds_" . $cdsPct->getPartName);
  $cdsStacked->setPlotTitle($cdsStacked->getPlotTitle() . " - cds");
  $cdsPct->setPlotTitle($cdsPct->getPlotTitle() . " - cds");

  $self->setGraphObjects($transcriptStacked, $cdsStacked);

}
1;
