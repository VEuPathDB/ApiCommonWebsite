package ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrSingleBar;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['blue'];

  $self->setProfileSetsHash
    ({Cln_Clb => {profiles => ['Expression profiling of saccharomyces cerevisiae s288c Cln/Clb experiments'],
                   y_axis_label => 'Expression Value',
force_x_axis_label_horizontal => 1,
                   colors => $colors,
                   make_y_axis_fold_incuction => 1,
                   default_y_max => 1,
              default_y_min => -1,
                  },
     });

  return $self;
}



1;
