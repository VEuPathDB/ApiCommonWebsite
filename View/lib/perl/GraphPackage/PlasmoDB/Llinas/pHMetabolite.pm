package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Llinas::pHMetabolite;

use strict;
use vars qw( @ISA);

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

#  my $colors = ['dodgerblue', 'slateblue', 'forestgreen', '#2F4F4F', 'salmon', '#E9967A'];

  my @profileSetNames = (['Profiles of Metabolites from Llinas', 'values'],
			);
  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);

  my $massSpec = ApiCommonWebsite::View::GraphPackage::BarPlot::MassSpec->new(@_);
  $massSpec->setProfileSets($profileSets);
  $massSpec->setAggregationFunction('sum');
#  $massSpec->setColors($colors);
  $massSpec->setDefaultYMax(100);
#  $massSpec->setDefaultYMin(-1);

#  $massSpec->setElementNameMarginSize(4);
#  $massSpec->setScreenSize(300); # to increase height of graph

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
