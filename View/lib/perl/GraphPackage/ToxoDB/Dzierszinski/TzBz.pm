package ApiCommonWebsite::View::GraphPackage::ToxoDB::Dzierszinski::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['pink', 'purple'];

  my $legend = ['VEG CO2-starvation', 'Pru CO2-starvation'];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of VEG strain CO2-starvation bradyzoite inducing conditions (by Florence Dzierszinski)',
                           'expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions (by Florence Dzierszinski)',
                          ],
              y_axis_label => 'RMA Value (log2)',
              x_axis_label => 'Days',
              colors => $colors,
              plot_title => 'CO2-Starvation Bradyzoite Inducing Conditions (Pru and VEG)',
              default_y_max => 8,
              default_y_min => 6,
              default_x_min => 0,
             },
      pct => {profiles => ['expression profile percentiles of VEG strain CO2-starvation bradyzoite inducing conditions (by Florence Dzierszinski)',
                           'expression profile percentiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions (by Florence Dzierszinski)',
                          ],
              y_axis_label => 'percentile',
              x_axis_label => 'Days',
              colors => $colors,
              plot_title => 'CO2-Starvation Bradyzoite Inducing Conditions (Pru and VEG) percentiles',
              default_y_max => 50,
              default_y_min => 0,
              default_x_min => 0,
       }
     });





  return $self;
}



1;
