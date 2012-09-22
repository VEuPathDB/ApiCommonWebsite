package ApiCommonWebsite::View::GraphPackage::ToxoDB::Gregory::TgME49VegRnaSeqTS;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('green', '#DDDDDD');
  my @legend = ("Uniquely Mapped", "Non-Uniquely Mapped");

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});

  my @profileArray = (['T. gondii ME49 time series mRNA Illumina sequences aligned to the ME49 Genome.', '', ''],
                      ['T. gondii ME49 time series mRNA Illumina sequences aligned to the ME49 Genome.-diff', '', ''],
                     );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - T. gondii ME49 time series mRNA Illumina sequences aligned to the ME49 Genome.', '', '']]);

  my $stacked = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);
  $stacked->setPartName('coverage_ME49');
  $stacked->setPlotTitle($stacked->getPlotTitle() . " - ME49");
  $stacked->setElementNameMarginSize(6);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[0]]);
  $percentile->setPartName('percentile_ME49');
  $percentile->setPlotTitle($percentile->getPlotTitle() . " - ME49");
  $percentile->setElementNameMarginSize(6);


  my @profileArrayVeg = (['T. gondii VEG time series mRNA Illumina sequences aligned to the ME49 Genome.', '', ''],
                         ['T. gondii VEG time series mRNA Illumina sequences aligned to the ME49 Genome.-diff', '', ''],
                         );

  my $profileSetsVeg = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArrayVeg);
  my $percentileSetsVeg = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - T. gondii VEG time series mRNA Illumina sequences aligned to the ME49 Genome.', '', '']]);

  my $stackedVeg = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stackedVeg->setProfileSets($profileSetsVeg);
  $stackedVeg->setColors(\@colors);
  $stackedVeg->setPartName('coverage_VEG');
  $stackedVeg->setPlotTitle($stackedVeg->getPlotTitle() . " - VEG");
  $stackedVeg->setElementNameMarginSize(4);

  my $percentileVeg = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentileVeg->setProfileSets($percentileSetsVeg);
  $percentileVeg->setColors([$colors[0]]);
  $percentileVeg->setPartName('percentile_VEG');
  $percentileVeg->setPlotTitle($percentileVeg->getPlotTitle() . " - VEG");
  $percentileVeg->setElementNameMarginSize(4);

  $self->setGraphObjects($stacked, $percentile, $stackedVeg, $percentileVeg);

  return $self;


}



