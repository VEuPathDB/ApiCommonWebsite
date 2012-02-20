package ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrTS;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlotSet );
use ApiCommonWebsite::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  # the screen needs to be bigger than the defaults
  $self->setPlotWidth(700);
  $self->setScreenSize(400);

  my $colors = ['blue', '#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];
  my $pch = [15,24,20,23,25];

  my $legend = ['Cln/Clb', 'pheromone', 'elutriation', 'cdc15', 'Cho et al'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch, cols => 5});

  $self->setProfileSetsHash
    ({expr_val => {profiles => ['Expression profiling of saccharomyces cerevisiae s288c Cln/Clb experiments',
                           'Expression profiling of saccharomyces cerevisiae s288c pheromone experiments',
                           'Expression profiling of saccharomyces cerevisiae s288c elutriation experiments',
                           'Expression profiling of saccharomyces cerevisiae s288c cdc15 Experiments',
                           'Expression profiling of saccharomyces cerevisiae s288c microarray from Cho et al.',
                          ],
              y_axis_label => 'Expression Value',
              x_axis_label => 'Time Point',
              colors => $colors,
              points_pch => $pch,
              make_y_axis_fold_incuction => 1,
              default_y_max => 1,
              default_y_min => -1,
             },
     });

  return $self;
}

1;
