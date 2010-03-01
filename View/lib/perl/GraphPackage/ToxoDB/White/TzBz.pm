package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(3);

  my $legend = ["GT1", "ME49", "CTGara"];

  my $colors = ['#B22222', '#6A5ACD', '#87CEEB' ];
  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});


  $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditoins'],
              y_axis_label => 'RMA Value (log2)',
              colors => $colors,
              x_axis_labels => ['Tachyzoite', 'Compound 1', 'pH=8.2'],
              r_adjust_profile => 'profile = rbind(profile[1,1:3], profile[1,4:6], profile[1,7:9]);',
              force_x_axis_label_horizontal => 1,
              plot_title => 'Normal-tachyzoite vs. Induced-bradyzoite - 3 Tgondii Strains',
             },

      pct => {profiles => ['expression profile percentiles of three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditoins'],
              y_axis_label => 'percentile',
              colors => $colors,
              x_axis_labels => ['Tachyzoite', 'Compound 1', 'pH=8.2'],
              r_adjust_profile => 'profile = rbind(profile[1,1:3], profile[1,4:6], profile[1,7:9]);',
              force_x_axis_label_horizontal => 1,
              plot_title => 'Normal-tachyzoite vs. Induced-bradyzoite Percentiles - 3 Tgondii Strains',
             }
     });

  return $self;
}



1;
