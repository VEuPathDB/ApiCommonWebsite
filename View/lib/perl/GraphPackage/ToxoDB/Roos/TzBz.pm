package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24,20,23];

  my $colors = ['#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];

  my $legend = ['RH Alkaline', 'Pru Sodium Nitroprusside', 'Pru CO2-starvation', 'Pru Alkaline'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)',
                           'expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions',
                           'expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions',
                           'expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)'
                          ],
              y_axis_label => 'RMA Value (log2)',
              x_axis_label => 'Hours',
              colors => $colors,
              plot_title => 'Tachyzoites under Bradyzoite-inducing conditions (Pru and RH)',
              default_y_max => 10,
              default_y_min => 4,
              points_pch => $pch,
             },
      pct => {profiles => ['expression profile percentiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)',
                           'expression profile percentiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions',
                           'expression profile percentiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions',
                           'expression profile percentiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)'
                          ],
              y_axis_label => 'percentile',
              x_axis_label => 'Hours',
              colors => $colors,
              plot_title => 'Tachyzoites under Bradyzoite-inducing conditions (Pru and RH) percentiles',
              default_y_max => 50,
              default_y_min => 0,
              points_pch => $pch,
       }
     });

  return $self;
}



1;
