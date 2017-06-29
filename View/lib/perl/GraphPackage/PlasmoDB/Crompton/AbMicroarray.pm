package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Crompton::AbMicroarray;

use strict;
use vars qw( @ISA);

use Data::Dumper;

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::GGScatterPlot;

sub init {
  my $self = shift;

  $self->setFacets(["ICEMR_health_status"]);
  $self->setContXAxis("PATO_0000011");

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $colors = ['blue','white'];

  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();

  my @profileSetArray = (['Crompton Ab Microarray Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = ApiCommonWebsite::View::GraphPackage::GGScatterPlot::LogRatio->new(@_);
#  my $scatter = ApiCommonWebsite::View::GraphPackage::ScatterPlot::ClinicalMetaData->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);


  # TODO: Remove this when we get facet and cont x dynamically
  $scatter->setXaxisLabel("Age");

  $self->setGraphObjects($scatter);

  return $self;

}
