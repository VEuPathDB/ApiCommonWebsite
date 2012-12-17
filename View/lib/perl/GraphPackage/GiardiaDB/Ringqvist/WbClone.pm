package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Ringqvist::WbClone;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#B22222', '#6A5ACD', '#87CEEB' ];
  my $pctColors = ['#B22222', '#191970', '#6A5ACD', '#191970', '#6A5ACD', '#191970', '#6A5ACD', '#191970', '#87CEEB','#191970', '#87CEEB','#191970', '#87CEEB','#191970'];
  my $legend =  ['DMEM', 'TYDK', 'Caco'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});


  my @profileSetsArray = (['Host Parasite Interaction', 'standard error - Host Parasite Interaction', ]);
  my @percentileSetsArray = (['red percentile - Host Parasite Interaction', '',],
                             ['green percentile - Host Parasite Interaction', '',]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($pctColors);

  $self->setGraphObjects($ratio, $percentile,);

  return $self;

}



1;
