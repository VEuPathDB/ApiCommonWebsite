package ApiCommonWebsite::View::GraphPackage::ToxoDB::Matrajt::BzTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(300);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(9);

  my $colors = ['#FF0000', '#FF6600','#FFFF00', '#009900', '#0000CC',];

  my $legend = ['Extracellular\nTachyzoite (0 hrs)', 'Bradyzoite (24 hrs)', 'Bradyzoite (36 hrs)',' Bradyzoite (48 hrs)', 'Bradyzoite (72 hrs)'];

  $self->setMainLegend({colors => ['#FF0000', '#FF6600','#FFFF00', '#009900', '#0000CC'], short_names => $legend, cols=> 3});

    $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of TgRH_Matrajt_GSE23174_Bz_Time_Series'],
              stdev_profiles => ['expression profile standard errors of TgRH_Matrajt_GSE23174_Bz_Time_Series'],
               #  x_axis_labels => [],
               y_axis_label => 'RMA Value (log2)',
               colors => $colors,
             },
      pct => {profiles => ['expression profile percentiles of TgRH_Matrajt_GSE23174_Bz_Time_Series'],
              # x_axis_labels => [],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
