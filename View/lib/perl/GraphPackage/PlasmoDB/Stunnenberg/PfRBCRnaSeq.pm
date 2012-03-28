package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Stunnenberg::PfRBCRnaSeq;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlotSet );
use ApiCommonWebsite::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  $self->setScreenSize(250);
#  $self->setBottomMarginSize(8);
#  $self->setLegendSize(60);

  my $pch = [19,24];

  my $colors = ['#E9967A', '#4682B4'];

  my $legend = ['Normal', 'Scaled'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['missing',
                                'Scaled missing',
                               ],
                   y_axis_label => 'Normalized Coverage (log2)',
                   x_axis_label => 'Hours Post Infection',
                   default_y_max => 15,
                   default_y_min => 0,
                   r_adjust_profile => 'profile[profile < 1] = 1; profile = log2(profile); ',
                   colors => $colors,
                   points_pch => $pch,
                  },
      pct => {profiles => ['Percents of P. falciparum Stunnenberg mRNA Seq data'],
              y_axis_label => 'Percentile',
              x_axis_label => 'Hours Post Infection',
              default_y_max => 50,
              default_y_min => 0,
              colors => ['#E9967A'],
              points_pch => [19],
             },
     });

  return $self;
}

1;
