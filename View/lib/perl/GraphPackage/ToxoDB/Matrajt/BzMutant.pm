package ApiCommonWebsite::View::GraphPackage::ToxoDB::Matrajt::BzMutant;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(300);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(7);

  my $colors = ['#996600', '#996600', '#996600', '#FF0000', '#FF0000', '#FF6600', '#FF6600','#FFFF00', '#FFFF00','#33FF66', '#33FF66', '#009900', '#009900', '#0000CC', '#0000CC', '#660033', '#660033',];

  my $legend = ['Wild Type', '12K Mutant', '13P Mutant', 'B7 Mutant', '11P Mutant', '11K Mutant', '7K Mutant', 'P11 Mutant',];

  $self->setMainLegend({colors => ['#996600', '#FF0000', '#FF6600','#FFFF00','#33FF66', '#009900', '#0000CC', '#660033',], short_names => $legend, cols=> 5});

    $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant'],
              stdev_profiles => ['expression profile standard errors of TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant'],
               #  x_axis_labels => [],
               y_axis_label => 'RMA Value (log2)',
               colors => $colors,
             },
      pct => {profiles => ['expression profile percentiles of TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant'],
              # x_axis_labels => [],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
