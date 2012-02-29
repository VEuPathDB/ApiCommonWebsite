package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Hehl::Stress2;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(4);

  my $xAxisLabels = ['Control', '30 min', '60 min', '120 min'];

  $self->setProfileSetsHash
    ({'expr_val' => {profiles => ['Stress response Dynamics in Trophozoites (time series)-Averaged'],
                     y_axis_label => 'Expression Value',
                     colors => ['darkgreen'],
                     make_y_axis_fold_incuction => 1,
                     default_y_max => 1,
                     default_y_min => -1,
                     x_axis_labels => $xAxisLabels,
                          },
      pct => {profiles => ['Stress Response percentiles by varying DTT incubation time-red values',
                           'Stress Response percentiles by varying DTT incubation time-green values'
                          ],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors =>  ['grey', '#191970'],
              x_axis_labels => $xAxisLabels,
             },
     });

  return $self;
}



1;
