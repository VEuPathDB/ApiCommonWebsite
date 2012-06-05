package ApiCommonWebsite::View::GraphPackage::FungiDB::CryptococcusNeoformansGrubiiH99::MicroarrayCneoToc1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#29ACF2', '#DDDDDD'];

  my @profileSetNames = (['Toc1, Toc2, and Skn7 null mutants treated with Flucytosine', 'standard error - Toc1, Toc2, and Skn7 null mutants treated with Flucytosine']);
  my @percentileSetNames = (['red percentile - Toc1, Toc2, and Skn7 null mutants treated with Flucytosine'],
                            ['green percentile - Toc1, Toc2, and Skn7 null mutants treated with Flucytosine']
                           );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);

  $self->setGraphObjects($ratio, $percentile);

#  my $legend = ['GSM746753_150_WT_-flu__sample_1_.txt','GSM746754_149_tco1_-flu__sample_1_.txt'];

#  $self->setMainLegend({colors => $colors, short_names => $legend, cols=> 3});


  return $self;
}

1;
