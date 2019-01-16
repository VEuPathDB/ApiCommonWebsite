package ApiCommonWebsite::View::GraphPackage::EuPathDB::Sullivan::GCN5A;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#D87093', '#D87093','#87CEEB', '#87CEEB'];
  my $legendColors = [@$colors[1..2]];

  my $legend = ["Wild Type", "GCN5-A Knockout", ];
  my $elementNames = ["WT:Stressed","WT:Unstressed","KO:Stressed","KO:Unstressed"];

  my @profileSetsArray = (['Toxoplasma gondii GCN5-A Knockout Array from Sullivan', 'standard error - Toxoplasma gondii GCN5-A Knockout Array from Sullivan', $elementNames]);
  my @percentileSetsArray = (['percentile - Toxoplasma gondii GCN5-A Knockout Array from Sullivan', '',$elementNames],);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  $self->setMainLegend({colors => $legendColors, short_names => $legend, cols=> 2});


  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (7);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (7);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}


1;
