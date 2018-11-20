package ApiCommonWebsite::View::GraphPackage::EuPathDB::Waters::Ver2;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;


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

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);

  my $ratio = EbrcWebsiteCommon::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0], $colors->[2]]);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);

  $self->setGraphObjects($ratio, $percentile);

  return $self;
}



1;
