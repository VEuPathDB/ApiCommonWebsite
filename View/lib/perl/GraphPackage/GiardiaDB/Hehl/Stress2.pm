package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Hehl::Stress2;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen', 'grey'];

  my @profileSetsArray = (['Stress Response profiles by varying DTT incubation time', 'standard error - Stress Response profiles by varying DTT incubation time', ]);
  my @percentileSetsArray = (['red percentile - Stress Response profiles by varying DTT incubation time', '',],
                             ['green percentile - Stress Response profiles by varying DTT incubation time', '',]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);

  $self->setGraphObjects($ratio, $percentile,);

  return $self;
}



1;
