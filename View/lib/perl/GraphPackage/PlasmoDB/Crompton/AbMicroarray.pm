package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Crompton::AbMicroarray;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::ScatterPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(500);

  my $metaDataCategory = $self->getTypeArg();
  my $colors = ['blue', 'white'];

  my @profileSetArray = (['Crompton Ab Microarray Profiles','','','','',$metaDataCategory]);
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = ApiCommonWebsite::View::GraphPackage::ScatterPlot->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setPlotTitle("Expression Values");
  $scatter->setDefaultYMax(4);
  $scatter->setDefaultYMin(-4);
  $scatter->setElementNameMarginSize(4);
  $scatter->setColors($colors);
  $scatter->setHasExtraLegend(1);
  $scatter->setExtraLegendSize(7);
  $scatter->setHasMetaData(1);

  $self->setGraphObjects($scatter);

  return $self;

}
