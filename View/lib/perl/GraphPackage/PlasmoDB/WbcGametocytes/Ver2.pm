package ApiCommonWebsite::View::GraphPackage::PlasmoDB::WbcGametocytes::Ver2;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('red','pink','purple');
  my @legend = ('3D7', 'MACS-purified 3D7', 'isolate NF54');

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 3});

  $self->setPlotWidth(450);

  my $absolute = ApiCommonWebsite::View::GraphPackage::LinePlot::RMA->new();
  $absolute->setProfileSetNames([' winzeler_3D7_gametocyte',
                                 ' winzeler_3D7_MAC',
                                 ' winzeler_NF54_gametocyte'
                                 ]);

  $absolute->setColors(\@colors);
  $absolute->setPointsPch([19,19,19]);
  $absolute->setPlotTitle("Expression Levels (log2 Absolute)");

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new();
  $percentile->setProfileSetNames(['percentile -  winzeler_3D7_gametocyte',
                                 'percentile -  winzeler_3D7_MAC',
                                 'percentile -  winzeler_NF54_gametocyte'
                                 ]);

  $percentile->setColors(\@colors);
  $percentile->setPointsPch([19,19,19]);
  $percentile->setPlotTitle("Expression Levels (Percentiled)");

  $self->setGraphObjects($absolute, $percentile);

  return $self;
}




1;
