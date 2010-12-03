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
    ({coverage => {profiles => ['T. brucei under heat shock array data','T. brucei DHH1 wild type array data','T. brucei DHH1 DEAD-DQAD mutant array data'],
                   y_axis_label => 'normalized coverage',
                   x_axis_labels => ['heat shock','wild type','mutant'],
                   colors => $colors,
                   # take transpose to change the columns to rows to columns, to get in-between spacing
                   r_adjust_profile => 'profile=t(profile);',
                   plot_title => 'T. brucei DHH1 wild type',
		   default_y_max => 1,
                   default_y_min => -1,
		   force_x_axis_label_horizontal => 1,
                  },
     });

  return $self;
}



1;

