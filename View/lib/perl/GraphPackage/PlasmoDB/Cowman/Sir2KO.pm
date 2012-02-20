package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Cowman::Sir2KO;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(6);

  my $colors = ['#6495ED', '#E9967A', '#2F4F4F' ];
  my $xAxisLabels = ['ring', 'trophozoite', 'schizont'];

  my $legend = ['Wild Type', 'sir2A', 'sir2B'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});


  $self->setProfileSetsHash
    ({rma => {profiles => ['Profiles of E-TABM-438 from Cowman'],
              y_axis_label => 'RMA Value (log2)',
              x_axis_labels => $xAxisLabels,
              colors => $colors,
              default_y_max => 6,
              r_adjust_profile => 'profile = rbind(profile[1,1:3], profile[1,4:6], profile[1,7:9]);',
              legend => ['Wild Type', 'sir2A KO', 'sir2B KO'],
             },
      pct => {profiles => ['Percentiles of of E-TABM-438 from Cowman'],
              y_axis_label => 'percentile',
              x_axis_labels => $xAxisLabels,
              colors => $colors,
              r_adjust_profile => 'profile = rbind(profile[1,1:3], profile[1,4:6], profile[1,7:9]);',
              legend => ['Wild Type', 'sir2A KO', 'sir2B KO'],
             },
      });

  return $self;
}


1;
