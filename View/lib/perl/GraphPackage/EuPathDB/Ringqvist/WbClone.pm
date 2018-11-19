package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Ringqvist::WbClone;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $legendColors = ['#B22222', '#6A5ACD', '#87CEEB' ];
  my $ratioColors = ['#B22222', '#6A5ACD', '#6A5ACD',  '#6A5ACD',  '#87CEEB', '#87CEEB', '#87CEEB',];
  my $pctColors = ['#B22222', '#191970', '#6A5ACD', '#191970', '#6A5ACD', '#191970', '#6A5ACD', '#191970', '#87CEEB','#191970', '#87CEEB','#191970', '#87CEEB','#191970'];
  my $legend =  ['DMEM', 'TYDK', 'Caco'];

  $self->setMainLegend({colors => $legendColors, short_names => $legend, cols => 3});


  my @profileSetsArray = (['Host Parasite Interaction', 'standard error - Host Parasite Interaction', ]);
  my @percentileSetsArray = (['red percentile - Host Parasite Interaction', '',],
                             ['green percentile - Host Parasite Interaction', '',]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio = EbrcWebsiteCommon::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors($ratioColors);
  $ratio->setElementNameMarginSize(6);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($pctColors);
  $percentile->setElementNameMarginSize(6);

  $self->setGraphObjects($ratio, $percentile,);

  return $self;

}



1;
