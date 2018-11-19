package ApiCommonWebsite::View::GraphPackage::EuPathDB::Kazura::AbMicroarray;

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
    $facet = ['EUPATH_0000048'];
  }
  my $needXLab = 0;
  if ($contXAxis eq 'na') {
    $contXAxis = 'OBI_0001169';
    $needXLab = 1;
  }

  my @profileSetArray = (['Kazura Reinfection Ab Microarray Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
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
