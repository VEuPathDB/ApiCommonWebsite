package ApiCommonWebsite::View::GraphPackage::EuPathDB::Helb::AbMicroarray;

use strict;
use vars qw( @ISA);

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot;
use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();

  my $colors = ['blue'];

  if ($facet->[0] eq 'na') {
    $facet->[0] = 'EUPATH_0005012';
  }
  my $needXLab = 0;
  if ($contXAxis eq 'na') {
    $contXAxis = 'EUPATH_0005032';
    $needXLab = 1;
  }

  my @profileSetArray = (['Recent exposure AB array profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot::LogRatio->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);

  if ($needXLab) {
    $scatter->setXaxisLabel("Age");
  }

  $self->setGraphObjects($scatter);

  return $self;

}
