package ApiCommonWebsite::View::GraphPackage::EuPathDB::Hehl::Encystation;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen', '#0066CC'];

  my @profileSetsArray = (['Hehl Encystation Expression', 'standard error - Hehl Encystation Expression', ]);
  my @percentileSetsArray = (['red percentile - Hehl Encystation Expression', '',],
                             ['green percentile - Hehl Encystation Expression', '',]);

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
