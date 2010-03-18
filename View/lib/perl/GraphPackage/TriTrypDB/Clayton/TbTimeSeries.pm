package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Clayton::TbTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#483D8B'];

  my $pch = [15];

  $self->setProfileSetsHash
    ({'fold_induction' => {profiles => ['Expression profiling of T. brucei microarray data from Christine Clayton'],
                           y_axis_label => 'Fold Induction',
                           x_axis_label => ' ',
                           colors => $colors,
                           make_y_axis_fold_incuction => 1,
                           plot_title => 'Expression profiling of T. brucei time series',
                           default_y_max => 2,
                           default_y_min => -2,
                           default_x_min => 0,
                           points_pch => $pch,
                          },

      pct => {profiles => ['Percentiles of T. brucei microarray data from Christine Clayton',
                          ],
              y_axis_label => 'Percentile',
              x_axis_label => ' ',
              colors => $colors,
              plot_title => 'Percentiles of T. brucei time series',
              default_y_max => 10,
              default_y_min => 4,
              default_x_min => 0,
              points_pch => $pch,
             },

     });

  return $self;
}



1;
