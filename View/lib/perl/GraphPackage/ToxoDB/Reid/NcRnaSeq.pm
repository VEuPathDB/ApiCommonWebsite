package ApiCommonWebsite::View::GraphPackage::ToxoDB::Reid::NcRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#66CDAA', '#D87093'];

  my $legend = ["day 3", "day 4"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['N. caninum Day 3-4 Tachyzoite-profiles'],
                   y_axis_label => 'log 2 (RPKM)',
                   force_x_axis_label_horizontal => 1,
                   colors => $colors,
                   default_y_max => 4,
                   x_axis_labels => $legend,
                   stdev_profiles => ['N. caninum Day 3-4 Tachyzoite-diff profiles'],
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);stdev=stdev + 1; stdev = log2(stdev);',
                  },
      pct => {profiles => ['N. caninum Day 3-4 Tachyzoite-percentiles'],
              y_axis_label => 'Percentile',
              force_x_axis_label_horizontal => 1,
              default_y_max => 50,
              colors => $colors,
              x_axis_labels => $legend,
              stdev_profiles => ['N. caninum Day 3-4 Tachyzoite-diff percentiles'],
             },
     });

  return $self;
}

1;
