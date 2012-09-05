package ApiCommonWebsite::View::GraphPackage::ToxoDB::Reid::NcRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#6A5ACD', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];

  my $xAxisLabels = ["day 3", "day 4"];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 2});

   my @profileSetsArray = (['N. caninum Day 3-4 Tachyzoite', '', ''],
                           ['N. caninum Day 3-4 Tachyzoite-diff', '', ''],
                          );
  my @percentileSetsArray = (['percentile - N. caninum Day 3-4 Tachyzoite', '','']);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $stacked = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors($colors);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([@$colors[0]]);

  $self->setGraphObjects($stacked, $percentile);

  return $self;
}

1;
