package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Newbold::IRBC;

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

  my @colors = ('#000080', '#DDDDDD');
  my @legend = ("Uniquely Mapped", "Non-Uniquely Mapped");

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});

  my @profileArray = (['P. falciparum Newbold mRNA Seq data'],
                      ['P. falciparum Newbold mRNA Seq data-diff'],
                     );

  my @sampleNames = (0,8,16,24,32,40,48);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - P. falciparum Newbold mRNA Seq data']]);


  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0]]);
  $line->setPartName('coverage_line');
  $line->setAdjustProfile('lines.df=lines.df + 1; lines.df = log2(lines.df);');
  $line->setYaxisLabel('RPKM (log2)');

  my $id = $self->getId();
  $line->setPlotTitle("Normalized Coverage - $id");

  my $stacked = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);
  $stacked->setSampleLabels(\@sampleNames);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[0]]);


  $self->setGraphObjects($line, $stacked, $percentile);

  return $self;
}




1;
