package ApiCommonWebsite::View::GraphPackage::ToxoDB::Matrajt::BzEgress;

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

  my $colors = ['#FF0000', '#FF6600', '#009900', '#0000CC',];

  my $legend = ['Extracellular Tachyzoite', 'Wild Type: Post-Egress', '13P Mutant: Post-Egress', 'B7 Mutant: Post-Egress',];

  $self->setMainLegend({colors => ['#FF0000', '#FF6600', '#009900', '#0000CC'], short_names => $legend, cols=> 2});

    $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of TgRH_Matrajt_GSE23174_Bz_11hr_Egress'],
              stdev_profiles => ['expression profile standard errors of TgRH_Matrajt_GSE23174_Bz_11hr_Egress'],
               #  x_axis_labels => [],
               y_axis_label => 'RMA Value (log2)',
               colors => $colors,
             },
      pct => {profiles => ['expression profile percentiles of TgRH_Matrajt_GSE23174_Bz_11hr_Egress'],
              # x_axis_labels => [],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
