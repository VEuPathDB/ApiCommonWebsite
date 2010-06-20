package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::CellCycle;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setBottomMarginSize(6.5);

  my $colors = ['#CD853F'];

  my $pch = [19];

  my $legend = ['Cell Cycle'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({rma => {profiles => ['M.White Cell Cycle Microarray profiles'
                          ],
              y_axis_label => 'RMA Value (log2)',
              x_axis_label => 'Time Point',
              colors => $colors,
              default_y_max => 10,
              default_y_min => 4,
              default_x_min => 0,
              points_pch => $pch,

             },
      pct => {profiles => ['M.White Cell Cycle Microarray profile pcts'
                          ],
              y_axis_label => 'percentile',
              x_axis_label => 'Time Point',
              colors => $colors,
              default_y_max => 50,
              default_y_min => 0,
              default_x_min => 0,
              points_pch => $pch,

       }
     });

  return $self;
}



1;
