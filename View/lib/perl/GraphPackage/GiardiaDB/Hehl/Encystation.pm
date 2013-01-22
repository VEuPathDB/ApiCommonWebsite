package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Hehl::Encystation;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen', '#0066CC'];

  my @profileSetsArray = (['Hehl Encystation Expression', 'standard error - Hehl Encystation Expression', ]);
  my @percentileSetsArray = (['red percentile - Hehl Encystation Expression', '',],
                             ['green percentile - Hehl Encystation Expression', '',]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);
  $ratio->setElementNameMarginSize(6);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize(6);

  $self->setGraphObjects($ratio, $percentile,);

  return $self;
}



1;
