package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Su::CQTreatment;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(8);

  my $colors = ['#F08080', '#7CFC00' ];

  my $legend = ['untreated', 'chloroquine'];
  my $pch = [22];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({rma => {profiles => ['Profiles of E-GEOD-10022 array from Su'],
              y_axis_label => 'RMA Value (log2)',
              x_axis_labels => ['106/1', '106/1(76I)', '106/1(76I_352K)'],
              colors => $colors,
              plot_title => 'Drug-selected mutants and short term CQ treatment',
              r_adjust_profile => 'profile = cbind(profile[1, 1:2],profile[1,3:4], profile[1,5:6]);',
              legend => [],
             },
      pct => {profiles => ['Percents of E-GEOD-10022 array from Su'],
              y_axis_label => 'percentile',
              x_axis_labels => ['106/1', '106/1(76I)', '106/1(76I_352K)'],
              colors => $colors,
              plot_title => 'Percentiles of Drug-selected mutants and short term CQ treatment',
              r_adjust_profile => 'profile = profile * 100;profile = cbind(profile[1, 1:2],profile[1,3:4], profile[1,5:6]);',
              legend => [],
             },
      });

  return $self;
}


1;
