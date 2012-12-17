package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Troell::CellCycle;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen'];

  my @profileSetsArray = (['Troell Cell Cycle', 'standard error - Troell Cell Cycle', ]);
  my @percentileSetsArray = (['percentile - Troell Cell Cycle', '',]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $expression = ApiCommonWebsite::View::GraphPackage::BarPlot::QuantileNormalized->new(@_);
  $expression->setProfileSets($profileSets);
  $expression->setColors([$colors->[0]]);
  $expression->setElementNameMarginSize (5);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (5);

  $self->setGraphObjects($expression, $percentile,);

  return $self;
}



1;
