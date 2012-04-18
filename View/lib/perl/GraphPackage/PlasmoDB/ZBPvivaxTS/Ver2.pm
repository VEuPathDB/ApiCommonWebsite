package ApiCommonWebsite::View::GraphPackage::PlasmoDB::ZBPvivaxTS::Ver2;

use vars qw( @ISA );

use strict;

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('#8A2BE2', '#20B2AA', '#808000' );
  my $legend = ['Patient 1', 'Patient 2', 'Patient 3'];

  $self->setMainLegend({colors => \@colors, short_names => $legend, cols => 3});

  my $sampleNames = [9, 13, 17, 20, 23, 29, 35, 40, 43];

  my @profileSetsArray = (['ZB Pvivax time series 1', '', $sampleNames],
                          ['ZB Pvivax time series 2', '', $sampleNames],
                          ['ZB Pvivax time series 3', '', $sampleNames],
                         );

  my @percentileSetsArray = (['red percentile - ZB Pvivax time series 1', '', $sampleNames],
#                             ['green percentile - ZB Pvivax time series 1', '', $sampleNames],
                             ['red percentile - ZB Pvivax time series 2', '', $sampleNames],
#                             ['green percentile - ZB Pvivax time series 2', '', $sampleNames],
                             ['red percentile - ZB Pvivax time series 3', '', $sampleNames],
#                             ['green percentile - ZB Pvivax time series 3', '', $sampleNames],
                            );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $line->setProfileSets($profileSets);
  $line->setColors(\@colors);
  $line->setPointsPch([15,15,15]);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors(\@colors);
  $percentile->setSpaceBetweenBars(.8);

  $self->setGraphObjects($line, $percentile);

  return $self;


}

1;


