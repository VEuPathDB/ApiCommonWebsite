package EbrcWebsiteCommon::View::GraphPackage::PlasmoDB::Stunnenberg::PfRBCRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  my $pch = [19,24];
  my $colors = ['#E9967A', '#4682B4', '#DDDDDD'];
  my $legend = ['Normal - Uniquely Mapped', 'Scaled - Uniquely Mapped', 'Non-Uniquely Mapped'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 2});

  my @profileArray = (['pfal3D7_Stunnenberg_pi_time_series'],
                      ['pfal3D7_Stunnenberg_pi_time_series - diff'],
                      ['pfal3D7_Stunnenberg_pi_time_series_scaled'],
                      ['pfal3D7_Stunnenberg_pi_time_series_scaled-diff'],
                     );


  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['percentile - pfal3D7_Stunnenberg_pi_time_series']]);

  my $line = EbrcWebsiteCommon::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0],$profileSets->[2]]);
  $line->setPartName('rpkm_line');
  $line->setAdjustProfile('lines.df=lines.df + 1; lines.df = log2(lines.df);');
  $line->setYaxisLabel('RPKM (log2)');
  $line->setPointsPch($pch);
  $line->setColors([$colors->[0], $colors->[1]]);

  my $id = $self->getId();
  $line->setPlotTitle("RPKM - $id");


  my @sampleLabels = (5,10,15,20,25,30,35,40);

  my $stacked = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stacked->setProfileSets([$profileSets->[0], $profileSets->[1]]);
  $stacked->setColors([$colors->[0],$colors->[2]] );
  $stacked->setSampleLabels(\@sampleLabels);

  my $stackedScaled = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stackedScaled->setProfileSets([$profileSets->[2], $profileSets->[3]]);
  $stackedScaled->setColors([$colors->[1],$colors->[2]] );
  $stackedScaled->setPartName("scaled_rpkm");
  $stackedScaled->setSampleLabels(\@sampleLabels);
  $stackedScaled->setPlotTitle("Scaled " . $stackedScaled->getPlotTitle());

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors->[0]]);


  $self->setGraphObjects($line, $stacked, $stackedScaled, $percentile);

  return $self;
}




1;
