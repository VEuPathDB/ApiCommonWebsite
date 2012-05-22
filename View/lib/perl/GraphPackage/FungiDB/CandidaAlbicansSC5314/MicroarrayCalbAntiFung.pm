package ApiCommonWebsite::View::GraphPackage::FungiDB::CandidaAlbicansSC5314::MicroarrayCalbAntiFung;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#29ACF2', '#DDDDDD'];

  my @profileSetNames = (['C.albicans activity against antifungal', 'standard error - C.albicans activity against antifungal']);
  my @percentileSetNames = (['red percentile - C.albicans activity against antifungal'],
                            ['green percentile - C.albicans activity against antifungal']
                           );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
#  $ratio->setColors([$colors->[0]]);
#  $ratio->setForceHorizontalXAxis(1);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
#  $percentile->setColors($colors);
#  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($ratio, $percentile);

  my $legend = ['GSM539775.txt', 'GSM539776.txt', 'GSM539777.txt', 'GSM539778.txt', 'GSM539779.txt', 'GSM539780.txt'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols=> 3});


  return $self;
}

1;
