package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Su::CQTreatment;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  $self->setScreenSize(200);
#  $self->setBottomMarginSize(8);

  my $colors = ['#F08080', '#7CFC00' ];

  my $legend = ['untreated', 'chloroquine'];
  my $pch = [22];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({rma => {profiles => ['E-GEOD-10022 array from Su'],
              y_axis_label => 'RMA Value (log2)',
              x_axis_labels => ['106/1', '106/1(76I)', '106/1(76I_352K)'],
              colors => $colors,
              r_adjust_profile => 'profile = cbind(profile[1, 1:2],profile[1,3:4], profile[1,5:6]);',
              force_x_axis_label_horizontal => 1, 
             legend => [],
             },
      pct => {profiles => ['percentile - E-GEOD-10022 array from Su'],
              y_axis_label => 'percentile',
              x_axis_labels => ['106/1', '106/1(76I)', '106/1(76I_352K)'],
              colors => $colors,
              r_adjust_profile => 'profile = cbind(profile[1, 1:2],profile[1,3:4], profile[1,5:6]);',
              force_x_axis_label_horizontal => 1, 
              legend => [],
             },
      });

  return $self;
}


1;
