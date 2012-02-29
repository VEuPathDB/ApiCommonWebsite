package ApiCommonWebsite::View::GraphPackage::ToxoDB::Carruthers::IntraExtraDiff;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(6);

  my $colors = ['#D87093','#E9967A', '#87CEEB'];

  my $legend = ["Extracelluar", "Intracelluar(0hr)","Intracelluar(2hr)", ];

  $self->setMainLegend({colors => ['#D87093','#E9967A', '#87CEEB'], short_names => $legend, cols=> 3});

    $self->setProfileSetsHash
    ({rma => {profiles => ['Expression profiles of Tgondii ME49 Carruthers experiments'],
              # x_axis_labels => [],
              y_axis_label => 'RMA Value (log2)',
               colors => $colors,
             },
      pct => {profiles => ['Expression percentile profiles of Tgondii ME49 Carruthers experiments'],              # x_axis_labels => [],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
