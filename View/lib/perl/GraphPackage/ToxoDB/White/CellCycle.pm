package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::CellCycle;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlotSet );
use ApiCommonWebsite::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setBottomMarginSize(6.5);

  my $colors = ['#CD853F'];

  my $pch = [19];

  my $legend = ['Cell Cycle'];


  my $cellCycleTopMargin = "
lines(c(2,5.75), c(y.max + (y.max - y.min)*0.1, y.max + (y.max - y.min)*0.1)); 
text(4, y.max + (y.max - y.min)*0.16, 'S(1)');

lines(c(5,6.9), c(y.max + (y.max - y.min)*0.125, y.max + (y.max - y.min)*0.125));
text(5.3, y.max + (y.max - y.min)*0.2, 'M');
text(6.3, y.max + (y.max - y.min)*0.2, 'C');

lines(c(6.1,10.4), c(y.max + (y.max - y.min)*0.1, y.max + (y.max - y.min)*0.1));
text(8.5, y.max + (y.max - y.min)*0.16, 'G1');

lines(c(10,13.2), c(y.max + (y.max - y.min)*0.125, y.max + (y.max - y.min)*0.125));
text(11.25, y.max + (y.max - y.min)*0.2, 'S(2)');

lines(c(12,14), c(y.max + (y.max - y.min)*0.15, y.max + (y.max - y.min)*0.15));
text(12.3, y.max + (y.max - y.min)*0.22, 'M');
text(13.3, y.max + (y.max - y.min)*0.22, 'C');


";

  $self->setProfileSetsHash
    ({rma => {profiles => ['M.White Cell Cycle Microarray profiles'
                          ],
              y_axis_label => 'RMA Value (log2)',
              x_axis_label => 'Time Point',
              colors => $colors,
              default_y_max => 10,
              default_y_min => 5,
              default_x_min => 0,
              points_pch => $pch,
              smooth_spline => 1,
              spline_approx_n => 60,
              r_top_margin_title => $cellCycleTopMargin,
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
              smooth_spline => 1,
              spline_approx_n => 60,
              r_top_margin_title => $cellCycleTopMargin,
       }
     });

  return $self;
}

1;
