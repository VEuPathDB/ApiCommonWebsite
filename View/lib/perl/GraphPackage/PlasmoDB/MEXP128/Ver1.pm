
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP128::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
#  $self->setBottomMarginSize(6);

  my $colors = ['#A52A2A', '#B0C4DE','#483D8B'],
  my $xAxisLabels = ['ring', 'trophozoite', 'schizont'];

  $self->setMainLegend({colors => $colors, short_names => $xAxisLabels, cols=>3});


  $self->setProfileSetsHash
    ({rma => {profiles => ['Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4 at ring, trophozite and schizont stages.'],
              y_axis_label => 'Expression Value (log2)',
              x_axis_labels => $xAxisLabels,
              colors => $colors,
              default_y_min => -1.5,
              default_y_max => 1.5,
              force_x_axis_label_horizontal => 1,
             },
      pct => {profiles => ['Percentiles of 3D7 clones 3D7AH1S2 and 3D7S8.4 at 3 life stages Red',
                           'Percentiles of 3D7 clones 3D7AH1S2 and 3D7S8.4 at 3 life stages Green'
                          ],
              y_axis_label => 'percentile',
              default_y_min => 0,
              x_axis_labels => $xAxisLabels,
              colors => ['#A52A2A', '#FFDAB9', '#B0C4DE','#FFDAB9','#483D8B','#FFDAB9'],
              force_x_axis_label_horizontal => 1,
             },
      });

  return $self;
}


1;
