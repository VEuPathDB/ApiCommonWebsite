package ApiCommonWebsite::View::GraphPackage::EuPathDB::Carrington::TbHeatShock;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#CCCCCC'];
  my $legend = ["heat shock vs. Control Strain"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({expr_val => {profiles => ['T. brucei under heat shock array data'],
                   x_axis_labels => ['heat shock'],
                   y_axis_label => 'Expression Value',
                   colors => $colors,
                   # take transpose to change the columns to rows to columns, to get in-between spacing
                   r_adjust_profile => 'profile=t(profile);',
                   plot_title => 'Transcriptional profiling of T. brucei - expression value',
                   default_y_max => 1,
                   default_y_min => -1,
                   force_x_axis_label_horizontal => 1,
                   make_y_axis_fold_incuction => 1,
                  },
      pct => {profiles => ['T. brucei under heat shock array data profile percents (red)',
                           'T. brucei under heat shock array data profile percents (green)'],
              x_axis_labels => ['heat shock'],
              y_axis_label => 'Percentile',
              force_x_axis_label_horizontal => 1,
#              r_adjust_profile => 'profile=t(profile);profile = cbind(profile[1,1:2], profile[1,3:4],profile[1,5:6]);',
              colors => ['#CCCCCC','dark blue'],
              plot_title => 'Transcriptional profiling of T. brucei - percentile',
             },

     });

  return $self;
}



1;

