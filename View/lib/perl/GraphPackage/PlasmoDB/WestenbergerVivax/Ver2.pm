package ApiCommonWebsite::View::GraphPackage::PlasmoDB::WestenbergerVivax::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['black', 
                'black', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'black', 
                'DarkCyan',
                'DarkCyan',
                'DarkCyan',
                'DarkCyan',
               ];

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['westenberger vivax expression profile']]);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['percentile - westenberger vivax expression profile']]);

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize(7);


  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize(7);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;

