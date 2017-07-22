package ApiCommonWebsite::View::GraphPackage::PiroplasmaDB::Lau::Bbovis;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = [ '#D87093', 'darkblue'];

#kappe_profiles_averaged_over_all_channels
#percentile - kappe_profiles_averaged_over_all_channels
#standard error - kappe_profiles_averaged_over_all_channels

  my @profileSetsArray = (['Lau B.b. Virulent vs Attenuated', 'standard error - Lau B.b. Virulent vs Attenuated', '']);
  my @percentileSetsArray = (['percentile - Lau B.b. Virulent vs Attenuated', '',''],
                             );



  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = EbrcWebsiteCommon::View::GraphPackage::BarPlot::QuantileNormalized->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors($colors);
  $ratio->setElementNameMarginSize(7);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setSpaceBetweenBars(0.5);
  $percentile->setElementNameMarginSize(7);

  $self->setGraphObjects($ratio, $percentile);

  return $self;


}

1;



