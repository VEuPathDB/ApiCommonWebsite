package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Sage::McArthur;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

use ApiCommonWebsite::View::GraphPackage::Util;
use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @legendColors = ('#A52A2A', '#DEB887');
  my @colors;

  push (@colors, @legendColors[0 ..1]) for 1 .. 10;

  my $legend = ["sense", "antisense"];

  $self->setMainLegend({colors => \@legendColors, short_names => $legend});

  my @profileSetsArray = (['giar sage tag frequencies sense', 'values',''],
                          ['giar sage tag frequencies antisense', 'values','']);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $percents = ApiCommonWebsite::View::GraphPackage::BarPlot::SageTag->new(@_);
  $percents->setProfileSets($profileSets);
  $percents->setColors(\@legendColors);
  $percents->setElementNameMarginSize(8.5);

  $self->setGraphObjects($percents);

  return $self;

}



1;
