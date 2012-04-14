package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Newbold::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(480);

  my $colors = ['lightskyblue2', 'red2'];
  my $xAxisLabels = ['mild disease', 'severe disease'];

  $self->setMainLegend({colors => $colors, short_names => $xAxisLabels, cols=>2});

  my @allColors;
  foreach(1..8) {
    push @allColors, $colors->[0];
  }
  foreach(1..9) {
    push @allColors, $colors->[1];
  }

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['newbold gene profiles sorted mild-severe']]);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - newbold gene profiles sorted mild-severe']]);

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new();
  $rma->setProfileSets($profileSets);
  $rma->setColors(\@allColors);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new();
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors(\@allColors);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;
