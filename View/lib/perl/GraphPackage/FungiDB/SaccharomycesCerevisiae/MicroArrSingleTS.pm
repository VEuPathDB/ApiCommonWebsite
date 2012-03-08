package ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrSingleTS;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlotSet );
use ApiCommonWebsite::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];
  my $pch = [24,20,23,25];


  $self->setProfileSetsHash
    ({

      pheromone => {profiles => ['Expression profiling of saccharomyces cerevisiae s288c pheromone experiments'],
                   y_axis_label => 'Expression Value',
                   x_axis_label => 'Time Point',
                   colors => [$colors->[0]],
                   points_pch => [$pch->[0]],
                   make_y_axis_fold_incuction => 1,
                   default_y_max => 1,
                   default_y_min => -1,
                  },

      elutriation => {profiles => ['Expression profiling of saccharomyces cerevisiae s288c elutriation experiments'],
                   y_axis_label => 'Expression Value',
                   x_axis_label => 'Time Point',
                   colors => [$colors->[1]],
                   points_pch => [$pch->[1]],
                   make_y_axis_fold_incuction => 1,
                   default_y_max => 1,
                   default_y_min => -1,
                  },


      cdc15 => {profiles => ['Expression profiling of saccharomyces cerevisiae s288c cdc15 Experiments'],
                   y_axis_label => 'Expression Value',
                   x_axis_label => 'Time Point',
                   colors => [$colors->[2]],
                   points_pch => [$pch->[2]],
                   make_y_axis_fold_incuction => 1,
                   default_y_max => 1,
                   default_y_min => -1,
                  },

      Cho => {profiles => ['Expression profiling of saccharomyces cerevisiae s288c microarray from Cho et al.'],
                   y_axis_label => 'Expression Value',
                   x_axis_label => 'Time Point',
                   colors => [$colors->[3]],
                   points_pch => [$pch->[3]],
                   make_y_axis_fold_incuction => 1,
                   default_y_max => 1,
                   default_y_min => -1,
                  },
     });

  return $self;
}

1;
