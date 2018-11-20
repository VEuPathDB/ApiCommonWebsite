package ApiCommonWebsite::View::GraphPackage::EuPathDB::Hehl::Stress2;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen', 'grey'];

  my @profileSetsArray = (['Stress Response profiles by varying DTT incubation time', 'standard error - Stress Response profiles by varying DTT incubation time', ]);
  my @percentileSetsArray = (['red percentile - Stress Response profiles by varying DTT incubation time', '',],
                             ['green percentile - Stress Response profiles by varying DTT incubation time', '',]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = EbrcWebsiteCommon::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);
  $ratio->setElementNameMarginSize(6);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize(6);

  $self->setGraphObjects($ratio, $percentile,);

  return $self;
}



1;
