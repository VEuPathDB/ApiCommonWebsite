package EbrcWebsiteCommon::View::GraphPackage::EuPathDB::Newbold::Patients;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

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

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['newbold gene profiles sorted mild-severe']]);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['percentile - newbold gene profiles sorted mild-severe']]);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors(\@allColors);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors(\@allColors);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;
