package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Crompton::AbMicroarray;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::GGScatterPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $colors = ['blue','white'];

  my @profileSetArray = (['Crompton Ab Microarray Profiles','values']);
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = ApiCommonWebsite::View::GraphPackage::GGScatterPlot::LogRatio->new(@_);
#  my $scatter = ApiCommonWebsite::View::GraphPackage::ScatterPlot::ClinicalMetaData->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);

  $self->setGraphObjects($scatter);

  return $self;

}
