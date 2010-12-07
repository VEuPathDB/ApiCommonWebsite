package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Carrington::TbCombined;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#CCCCCC','#999999','#555555'];
  my $legend = ["heat shock","DHH1 wild type","DHH1 DEAD DQAD mutant"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({expr_val => {profiles => ['T. brucei under heat shock array data','T. brucei DHH1 wild type array data','T. brucei DHH1 DEAD-DQAD mutant array data'],
                   x_axis_labels => ['heat shock','wild type','mutant'],
                   y_axis_label => 'Expression Value',
                   colors => $colors,
                   # take transpose to change the columns to rows to columns, to get in-between spacing
                   r_adjust_profile => 'profile=t(profile);',
                   plot_title => 'Transcriptional profiling of T. brucei - expression value',
                   default_y_max => 1,
                   default_y_min => -1,
                   force_x_axis_label_horizontal => 1,
                  },
      pct => {profiles => ['T. brucei under heat shock array data profile percents (red)',
                           'T. brucei under heat shock array data profile percents (green)',
                           'T. brucei DHH1 wild type array data profile percents (red)',
                           'T. brucei DHH1 wild type array data profile percents (green)',
                           'T. brucei DHH1 DEAD-DQAD mutant array data profile percents (red)',
                           'T. brucei DHH1 DEAD-DQAD mutant array data profile percents (green)'],
              x_axis_labels => ['heat shock','wild type','mutant'],
              y_axis_label => 'Percentile',
              force_x_axis_label_horizontal => 1,
              r_adjust_profile => 'profile=t(profile);profile = cbind(profile[1,1:2], profile[1,3:4],profile[1,5:6]);',
              colors => ['#CCCCCC','dark blue','#999999','dark blue','#555555','dark blue'],
              plot_title => 'Transcriptional profiling of T. brucei - percentile',
             },

     });

  return $self;
}



1;

