package ApiCommonWebsite::View::GraphPackage::Examples::RNASeqPct;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::PercentilePlot;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#6495ED', '#E9967A', '#2F4F4F' ];
  my $legend = ['Wild Type', 'sir2A', 'sir2B'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});

  my $graphObject = EbrcWebsiteCommon::View::GraphPackage::PercentilePlot->new();

  $graphObject->setProfileSetNames(['Percentiles of of E-TABM-438 from Cowman']);
  $graphObject->setPartName('bar');
  $graphObject->setColors($colors);
  $graphObject->setPlotTitle('This is a test of the emergency graph generation system');
  
  my $GO1 = EbrcWebsiteCommon::View::GraphPackage::PercentilePlot->new();
  $GO1->setProfileSetNames(['Percentiles of of E-TABM-438 from Cowman']);
  $GO1->setColors($colors);
  $GO1->setPlotTitle('This is a test of the emergency graph generation system');

  my $GO2 = EbrcWebsiteCommon::View::GraphPackage::LinePlot->new();
  $GO2->setProfileSetNames(['Profiles of P.falciparum Newbold mRNA Seq data']);
  $GO2->setColors(['#000080']);
  $GO2->setPartName('line');
  $GO2->setPlotTitle('This is a test of the emergency graph generation system');
  $GO2->setYaxisLabel('Normalized Coverage (log2)');
  $GO2->setAdjustProfile('profile[profile < 1] = 1; profile = log2(profile); ');
  $GO2->setXaxisLabel('Hours');

$self->setGraphObjects($graphObject,$GO1,$GO2);

  return $self;
}


1;

