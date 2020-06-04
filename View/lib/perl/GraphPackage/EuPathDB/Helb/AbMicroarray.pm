package ApiCommonWebsite::View::GraphPackage::EuPathDB::Helb::AbMicroarray;

use strict;
use vars qw( @ISA);

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LegacyGGScatterPlot;
use Data::Dumper;

sub useLegacy { return 1; }

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();

  my $colors = ['blue'];

  if (!(defined $facet) || $facet->[0] eq 'na') {
    $facet->[0] = 'none';
  }

  my $needXLab = 0;
  if (!(defined $contXAxis) || $contXAxis eq 'na') {
    $contXAxis = 'EUPATH_0005029';
    $needXLab = 1;
  }

  my @profileSetArray = (['Recent exposure AB array profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = EbrcWebsiteCommon::View::GraphPackage::LegacyGGScatterPlot::LogRatio->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);

  if ($needXLab) {
    $scatter->setXaxisLabel("Malaria incidence in the previous 365 days, log transformed");
  }

  $self->setGraphObjects($scatter);

  return $self;

}
