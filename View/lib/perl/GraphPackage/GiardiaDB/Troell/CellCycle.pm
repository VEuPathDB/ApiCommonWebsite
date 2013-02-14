package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Troell::CellCycle;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen'];

  my @profileSetsArray = (['Troell Cell Cycle', 'standard error - Troell Cell Cycle', ]);
  my @percentileSetsArray = (['percentile - Troell Cell Cycle', '',]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $expression = ApiCommonWebsite::View::GraphPackage::LinePlot::QuantileNormalized->new(@_);
  $expression->setProfileSets($profileSets);
  $expression->setColors([$colors->[0]]);
  $expression->setElementNameMarginSize (5);
  $expression->setXaxisLabel("Hours");
  $expression->setAdjustProfile('lines.df = lines.df - lines.df[,1];');
  $expression->setDefaultYMin(-1);
  $expression->setDefaultYMax(1);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (5);
  $percentile->setXaxisLabel("Hours");

  $self->setGraphObjects($expression, $percentile,);

  return $self;
}



1;
