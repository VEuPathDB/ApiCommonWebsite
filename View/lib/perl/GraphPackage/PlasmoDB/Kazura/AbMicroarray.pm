package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Kazura::AbMicroarray;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::GGScatterPlot;
use Data::Dumper;

sub init {
  my $self = shift;

  $self->setFacets(["ICEMR_microscopy_result"]);
  $self->setContXAxis("OBI_0001169");


  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $colors = ['blue','white'];
  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();

  my @profileSetArray = (['Kazura Reinfection Ab Microarray Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
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
