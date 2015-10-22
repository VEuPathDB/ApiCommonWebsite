package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Kazura::AbMicroarray;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::ScatterPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $metaDataCategory = $self->getTypeArg();
  my $colors = ['blue','white'];

  my @profileSetArray = (['Kazura Reinfection Ab Microarray Profiles','','','','',$metaDataCategory]);
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = ApiCommonWebsite::View::GraphPackage::ScatterPlot::ClinicalMetaData->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);

  $self->setGraphObjects($scatter);

  return $self;

}
