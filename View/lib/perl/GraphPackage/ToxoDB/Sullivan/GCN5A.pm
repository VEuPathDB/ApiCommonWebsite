package ApiCommonWebsite::View::GraphPackage::ToxoDB::Sullivan::GCN5A;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(300);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(9);

  my $colors = ['#D87093', '#D87093','#87CEEB', '#87CEEB'];

  my $legend = ["Wild Type", "GCN5-A Knockout", ];

  $self->setMainLegend({colors => ['#D87093', '#87CEEB'], short_names => $legend, cols=> 2});

    $self->setProfileSetsHash
    ({rma => {profiles => ['Profiles of GSE22100 GCN5-AE-GEOD-10022 array from Sullivan'],
               #  x_axis_labels => [],
               y_axis_label => 'RMA Value (log2)',
               colors => $colors,
             },
      pct => {profiles => ['Percentiles of GSE22100 GCN5-AE-GEOD-10022 array from Sullivan'],
              # x_axis_labels => [],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
