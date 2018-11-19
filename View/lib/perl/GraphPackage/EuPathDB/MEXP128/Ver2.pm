package ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP128::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);

  my $colors = ['#A52A2A', '#B0C4DE','#483D8B'];
  my $xAxisLabels = ['ring', 'trophozoite', 'schizont'];

  $self->setMainLegend({colors => $colors, short_names => $xAxisLabels, cols=>3});

  my @profileArray = (['Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4', 'values', 'Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4', 'standard_error'],
                     );

  my @percentileArray = (['Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4', 'channel1_percentiles'],
                         ['Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4', 'channel2_percentiles'],
                        );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArray);


  my $loess = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::LogRatio->new(@_);
  $loess->setProfileSets($profileSets);
  $loess->setColors($colors);
  $loess->setForceHorizontalXAxis(1);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors(['#A52A2A', '#FFDAB9', '#B0C4DE','#FFDAB9','#483D8B','#FFDAB9']);
  $percentile->setForceHorizontalXAxis(1);
  $self->setGraphObjects($loess, $percentile);

  return $self;


}

1;



