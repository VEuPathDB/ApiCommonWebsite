package ApiCommonWebsite::View::GraphPackage::EuPathDB::Duffy::PfRnaSeq;


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

  my @colors = ('#E9967A', '#DDDDDD');
  my @legend = ("Uniquely Mapped", "Non-Uniquely Mapped");

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 3});

  my $elementNames = ['Pregnant Women', 'Children', '3D7'];

  my @profileArray = (['P.falciparum duffy mRNA Seq data', '', $elementNames],
                      ['P.falciparum duffy mRNA Seq data-diff', '', $elementNames],
                     );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['percentile - P.falciparum duffy mRNA Seq data', '', $elementNames]]);

  my $stacked = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);
  $stacked->setForceHorizontalXAxis(1);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[0]]);
  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($stacked, $percentile);

  return $self;
}




1;

