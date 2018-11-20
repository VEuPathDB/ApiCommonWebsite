package ApiCommonWebsite::View::GraphPackage::EuPathDB::Cowman::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $legendColors = ['red', 'green', 'blue' ];
  my $legend = ['merozoite invasion', 'SIR2 KO', 'sialic acid-dependent vs. -independent red cell receptor invasion'];

  my $colors = ['green', 'green', 'green', 'green', 'blue', 'blue', 'blue', 'red', 'red', 'red', 'red', 'red'];

  $self->setMainLegend({colors => $legendColors, short_names => $legend, cols => 1});

  my @profileArray = (['Profiles of Cowman Invasion KO-averaged', 'standard error - Profiles of Cowman Invasion KO-averaged']
                     );

  my @percentileArray = (['percentile - Profiles of Cowman Invasion KO-averaged']
                        );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setIsHorizontal(1);
  $rma->setElementNameMarginSize(10);
  $rma->setScreenSize(500);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setIsHorizontal(1);
  $percentile->setElementNameMarginSize(10);
  $percentile->setScreenSize(500);
  
  $self->setGraphObjects($rma, $percentile);

  return $self;






}

1;

