package ApiCommonWebsite::View::GraphPackage::EuPathDB::Hehl::Stress1;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen', 'grey'];

  my @profileSetsArray = (['Stress Response profiles by temperature or varying DTT concentrations', 'standard error - Stress Response profiles by temperature or varying DTT concentrations', ]);
  my @percentileSetsArray = (['red percentile - Stress Response profiles by temperature or varying DTT concentrations', '',],
                             ['green percentile - Stress Response profiles by temperature or varying DTT concentrations', '',]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = EbrcWebsiteCommon::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);
  $ratio->setElementNameMarginSize (6);
  $ratio->setScreenSize(250);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (6);

  $self->setGraphObjects($ratio, $percentile,);

  return $self;

}



1;
