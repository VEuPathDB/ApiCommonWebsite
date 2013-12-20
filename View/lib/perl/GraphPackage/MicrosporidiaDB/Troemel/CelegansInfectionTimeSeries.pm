package ApiCommonWebsite::View::GraphPackage::MicrosporidiaDB::Troemel::CelegansInfectionTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::SimpleRNASeqLinePlot;
use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::Util;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

#  $self->setXAxisLabel("hours");
  my @colors = ('#D87093', '#DDDDDD', '#D87093');
  my @legend = ("Uniquely Mapped", "Non-Uniquely Mapped");

  

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});

  # Draw the diff first in light grey ... then the min rpkm will go on top
  my @profileArray = (['Nematocida parisii ERTm1 Spores', '', ''],
                      ['C. elegans Time Series - Infected - diff', '', ''],
                      ['C. elegans Time Series - Infected', '', ''],

                     );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - Nematocida parisii ERTm1 Spores', '', ''],
                                                                                    ['percentile - C. elegans Time Series - Infected', '', '']
                                                                                   ]);


  my $additionalRCode = "lines.df[2,] = lines.df[2,] + lines.df[3,];";


  my $stacked = ApiCommonWebsite::View::GraphPackage::LinePlot::PairedEndRNASeq->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);

  $stacked->addAdjustProfile($additionalRCode);
  $stacked->setXaxisLabel("hours");
  $stacked->setPointsPch([19,'NA','NA']);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[0]]);

  $stacked->setElementNameMarginSize(6);
  $percentile->setElementNameMarginSize(6);


  $self->setGraphObjects($stacked, $percentile);


  return $self;
}

1;
