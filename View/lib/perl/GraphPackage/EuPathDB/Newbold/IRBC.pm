package EbrcWebsiteCommon::View::GraphPackage::EuPathDB::Newbold::IRBC;

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

  my @colors = ('#000080', '#DDDDDD');
  my @legend = ("Uniquely Mapped", "Non-Uniquely Mapped");

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});

  my @profileArray = (['P. falciparum Newbold mRNA Seq data'],
                      ['P. falciparum Newbold mRNA Seq data-diff'],
                     );

  my @sampleNames = (0,8,16,24,32,40,48);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['percentile - P. falciparum Newbold mRNA Seq data']]);


  my $line = EbrcWebsiteCommon::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0]]);
  $line->setPartName('rpkm_line');
  $line->setAdjustProfile('lines.df=lines.df + 1; lines.df = log2(lines.df);');
  $line->setYaxisLabel('RPKM (log2)');

  my $id = $self->getId();
  $line->setPlotTitle("RPKM - $id");

  my $stacked = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);
  $stacked->setSampleLabels(\@sampleNames);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[0]]);


  $self->setGraphObjects($line, $stacked, $percentile);

  return $self;
}




1;
