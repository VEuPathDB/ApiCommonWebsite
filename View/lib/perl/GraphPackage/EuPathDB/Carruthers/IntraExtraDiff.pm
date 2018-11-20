package ApiCommonWebsite::View::GraphPackage::EuPathDB::Carruthers::IntraExtraDiff;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  $self->setScreenSize(250);
#  $self->setPlotWidth(450);
#  $self->setBottomMarginSize(6);

  my $colors = ['#D87093','#E9967A', '#87CEEB'];

  my $legend = ["Extracelluar", "Intracelluar(0HR)", "Intracelluar(2HR)" ];

  my @profileSetsArray = (['Expression profiles of Tgondii ME49 Carruthers experiments', 'standard error - Expression profiles of Tgondii ME49 Carruthers experiments', '']);
  my @percentileSetsArray = (['percentile - Expression profiles of Tgondii ME49 Carruthers experiments', '',''],);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  $self->setMainLegend({colors => $colors, short_names => $legend, cols=> 3});

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setSampleLabels($legend);
  $rma->setElementNameMarginSize(7.5);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setSampleLabels($legend);
  $percentile->setElementNameMarginSize(7.5);


  $self->setGraphObjects($rma, $percentile);

  return $self;

}

1;
