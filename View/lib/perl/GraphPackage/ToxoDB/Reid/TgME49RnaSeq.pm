package ApiCommonWebsite::View::GraphPackage::ToxoDB::Reid::TgME49RnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(180);
  $self->setBottomMarginSize(4);

  my $colors =['#66CDAA', '#D87093'];

  my $legend = ["day 3", "day 4"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome-diff profiles'],
                   y_axis_label => 'normalized coverage',
                   colors => $colors,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   plot_title => 'T.gondii VEG Day 3-4 tachyzoite forms RNA Sequence Coverage',
                  },
      pct => {profiles => ['T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome-diff percentiles'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors => $colors,
              r_adjust_profile => 'profile = profile * 100;',
              plot_title => 'T.gondii VEG Day 3-4 tachyzoite forms RNA Sequence Coverage',
             },
     });

  return $self;
}

1;
