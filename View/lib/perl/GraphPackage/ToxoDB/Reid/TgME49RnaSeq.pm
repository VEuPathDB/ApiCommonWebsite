package ApiCommonWebsite::View::GraphPackage::ToxoDB::Reid::TgME49RnaSeq;

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
    ({coverage => {profiles => ['T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome-profiles'],
                   y_axis_label => 'RPKM',
                   force_x_axis_label_horizontal => 1,
                   colors => $colors,
                   default_y_max => 4,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   plot_title => 'T.gondii VEG Day 3-4 tachyzoite forms RNA Sequence Coverage',
                  },
      pct => {profiles => ['T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome-percentiles'],
              y_axis_label => 'Percentile',
              force_x_axis_label_horizontal => 1,
              default_y_max => 50,
              colors => $colors,
              r_adjust_profile => 'profile = profile;',
              plot_title => 'T.gondii VEG Day 3-4 tachyzoite forms RNA Sequence Coverage',
             },
     });

  return $self;
}

1;
