package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Hehl::Stress1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen', 'grey'];

  my @profileSetsArray = (['Stress Response profiles by temperature or varying DTT concentrations', 'standard error - Stress Response profiles by temperature or varying DTT concentrations', ]);
  my @percentileSetsArray = (['red percentile - Stress Response profiles by temperature or varying DTT concentrations', '',],
                             ['green percentile - Stress Response profiles by temperature or varying DTT concentrations', '',]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);
  $ratio->setElementNameMarginSize (6);
  $ratio->setScreenSize(250);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (6);

  $self->setGraphObjects($ratio, $percentile,);

  return $self;

}



1;
