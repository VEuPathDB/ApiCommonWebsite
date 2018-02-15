package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Kazura::AbMicroarray;

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

#  my $colors = ['blue','white'];
  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();

   my $colors = ['blue'];
#   my $facet = ['ICEMR_microscopy_result'];
#   my $contXAxis = 'OBI_0001169';

  my @profileSetArray = (['Kazura Reinfection Ab Microarray Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot::LogRatio->new(@_);
#  my $scatter = EbrcWebsiteCommon::View::GraphPackage::ScatterPlot::ClinicalMetaData->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);
#  $scatter->setXaxisLabel("Age");

  $self->setGraphObjects($scatter);

  return $self;

}
