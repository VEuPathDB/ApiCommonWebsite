package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Beverly::LmThreeDevStages;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(4);
  $self->setLegendSize(40);

  my $colors =['#D87093', '#D87093', '#D87093', '#98FB98', '#98FB98', '#98FB98'];

  my $legend = ["Mouse Lesion Derived Amastigote vs. Early Log Procyclic Promastigote", "PNA - Metacyclic Promastigote vs. Early Log Procyclic Promastigote"];

  $self->setMainLegend({colors => ['#D87093', '#98FB98'], short_names => $legend, cols => 1});

  $self->setProfileSetsHash
    ({'m' => {profiles => ['Profiles of L.major Beverly Steve array data'],
              y_axis_label => 'log2(R/G)',
              colors => $colors,
              r_adjust_profile => 'profile = cbind(profile[1,1:3], profile[1,4:6]);',
              default_y_max => 1.5,
              default_y_min => -1.5,
              force_x_axis_label_horizontal => 1,
              x_axis_labels => ['MLDA vs ELPP', 'PNAMP vs ELPP'],
              plot_title => 'Transcript profiling of 3 L.major developmental stages',
             },
      pct => {profiles => ['Percents of L.major Beverly Steve array data'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              r_adjust_profile => 'profile = cbind(profile[1,1:3], profile[1,4:6]);',
              colors => $colors,
              force_x_axis_label_horizontal => 1,
              x_axis_labels => ['MLDA vs ELPP', 'PNAMP vs ELPP'],
              plot_title => 'Transcript profiling of 3 L.major developmental stages Percentiles',
             },
     });

  return $self;
}



1;
