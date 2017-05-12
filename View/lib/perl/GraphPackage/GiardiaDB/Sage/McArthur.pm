package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Sage::McArthur;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::GGBarPlot;

use ApiCommonWebsite::View::GraphPackage::Util;
use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @legendColors = ('#A52A2A', '#DEB887');

  my $legend = ["sense", "antisense"];

  my @profileSetsArray = (['giar sage tag frequencies sense', 'values',''],
                          ['giar sage tag frequencies antisense', 'values','']);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $percents = ApiCommonWebsite::View::GraphPackage::GGBarPlot::SageTag->new(@_);
  $percents->setProfileSets($profileSets);
  $percents->setColors(\@legendColors);
  $percents->setElementNameMarginSize(8.5);


  $percents->setHasExtraLegend(1); 
  $percents->setLegendLabels($legend);


  $self->setGraphObjects($percents);

  return $self;

}



1;
