package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::halflife;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  $self->setScreenSize(200);
#  $self->setBottomMarginSize(3);

  my $legend = ['Ring', 'Trophozoite', 'Schizont', 'Late schiz.'];
  my $colors = ['purple', 'darkred', 'green', 'orange']; # as in the paper!
  my $xlabels = ['C',0,5,10,15,30,60,120,240];

  $self->setMainLegend({colors => $colors, short_names => $legend});


  $self->setProfileSetsHash
    ({half_life => {profiles => ['Profiles of Derisi HalfLife-half_life'],
#             stdev_profiles => ['Profiles of Derisi HalfLife-std_dev'],
              y_axis_label => 'half-life (min)',
              colors => $colors,
              x_axis_labels => ['Ring', 'Trophozoite', 'Schizont', 'Late schiz.'],
              force_x_axis_label_horizontal => 1,
              plot_title => 'Half-life',
             },

#       pctR => {profiles => ['Percentiles of Derisi HalfLife-Ring Red',
# 			    'Percentiles of Derisi HalfLife-Ring Green',
#                           ],
#               y_axis_label => 'Percentile',
#               colors => [$$colors[0],'gray'],
#               x_axis_labels => $xlabels,
#               force_x_axis_label_horizontal => 1,
#               plot_title => 'Half-life',
#               default_y_max => 50,
#               default_y_min => 0,
#              },

#       pctT => {profiles => ['Percentiles of Derisi HalfLife-Trophozoite Red',
# 			    'Percentiles of Derisi HalfLife-Trophozoite Green'
# 			   ],
#               y_axis_label => 'Percentile',
#               colors => [$$colors[1],'gray'],
#               x_axis_labels => $xlabels,
#               force_x_axis_label_horizontal => 1,
#               plot_title => 'Half-life',
#               default_y_max => 50,
#               default_y_min => 0,
#              },

#       pctS => {profiles => ['Percentiles of Derisi HalfLife-Schizont Red',
# 			    'Percentiles of Derisi HalfLife-Schizont Green'
# 			   ],
#               y_axis_label => 'Percentile',
#               colors => [$$colors[2],'gray'],
#               x_axis_labels => $xlabels,
#               force_x_axis_label_horizontal => 1,
#               plot_title => 'Half-life',
#               default_y_max => 50,
#               default_y_min => 0,
#              },

#       pctLS => {profiles => ['Percentiles of Derisi HalfLife-Late_Schizont Red',
# 			      'Percentiles of Derisi HalfLife-Late_Schizont Green'
# 			   ],
#               y_axis_label => 'Percentile',
#               colors => [$$colors[3],'gray'],
#               x_axis_labels => $xlabels,
#               force_x_axis_label_horizontal => 1,
#               plot_title => 'Half-life',
#               default_y_max => 50,
#               default_y_min => 0,
#              },

     });

  return $self;
}


1;
