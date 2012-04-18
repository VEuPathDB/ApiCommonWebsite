package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Waters::Ver2;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#B22222', 'darkblue', '#8B008B', 'darkblue' ];

  my @legendColors = ($colors->[0], $colors->[2], $colors->[1]);
  my @legend = ("HP", "HPE", "Control");

  $self->setMainLegend({colors => \@legendColors, short_names => \@legend, cols => 3});


  my @profileSetNames = (['Waters HP'],
                         ['Waters HPE']
                        );

  my @percentileSetNames = (['red percentile - Waters HP'],
                            ['green percentile - Waters HP'],
                            ['red percentile - Waters HPE'],
                            ['green percentile - Waters HPE'],
                           );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0], $colors->[2]]);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);

  $self->setGraphObjects($ratio, $percentile);

  return $self;
}



1;
