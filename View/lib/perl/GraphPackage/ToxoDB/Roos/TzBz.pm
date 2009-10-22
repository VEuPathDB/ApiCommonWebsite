package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  $self->setProfileSetsHash
    ({
rma => {profiles => ['expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)',
                           'expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions',
                           'expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions',
                           'expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)'
                          ],
              y_axis_label => 'RMA Value (log2)',
              x_axis_label => 'Hours',
              colors => ['pink', 'purple', 'brown', 'blue'],
              plot_title => 'Tachyzoites under Bradyzoite-inducing conditions (Pru and RH)',
              default_y_max => 8,
              default_y_min => 6,
             },
      pct => {profiles => ['expression profile percentiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)',
                           'expression profile percentiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions',
                           'expression profile percentiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions',
                           'expression profile percentiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)'
                          ],
              y_axis_label => 'percentile',
              x_axis_label => 'Hours',
              colors => ['pink', 'purple', 'brown', 'blue'],
              plot_title => 'Tachyzoites under Bradyzoite-inducing conditions (Pru and RH) percentiles',
              default_y_max => 50,
              default_y_min => 0,
       }
     });

  return $self;
}



1;
