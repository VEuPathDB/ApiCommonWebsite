package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Crompton::AbMicroarray;

use strict;
use vars qw( @ISA);

use Data::Dumper;

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $colors = ['blue'];
  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();
  if ($facet->[0] eq 'na') {
    $facet->[0] = 'OGMS_0000073';
  }
  my $needXLab = 0;
  if ($contXAxis eq 'na') {
    $contXAxis = 'OBI_0001169';
    $needXLab = 1;
  }

  my @profileSetArray = (['Crompton Ab Microarray Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
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
