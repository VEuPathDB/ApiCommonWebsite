package ApiCommonWebsite::View::GraphPackage::ToxoDB::Dzierszinski::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#CD853F', '#8FBC8F'];

  my $pch = [19,24];

  my $legend = ['VEG CO2-starvation', 'Pru CO2-starvation'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of VEG strain CO2-starvation bradyzoite inducing conditions',
                           'expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions'
                          ],
              y_axis_label => 'RMA Value (log2)',
              x_axis_label => 'Days',
              colors => $colors,
              plot_title => 'CO2-Starvation Bradyzoite Inducing Conditions (Pru and VEG)',
              default_y_max => 10,
              default_y_min => 4,
              default_x_min => 0,
              points_pch => $pch,
             },
      pct => {profiles => ['expression profile percentiles of VEG strain CO2-starvation bradyzoite inducing conditions',
                           'expression profile percentiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions'
                          ],
              y_axis_label => 'percentile',
              x_axis_label => 'Days',
              colors => $colors,
              plot_title => 'CO2-Starvation Bradyzoite Inducing Conditions (Pru and VEG) percentiles',
              default_y_max => 50,
              default_y_min => 0,
              default_x_min => 0,
              points_pch => $pch,
       }
     });





  return $self;
}



1;
