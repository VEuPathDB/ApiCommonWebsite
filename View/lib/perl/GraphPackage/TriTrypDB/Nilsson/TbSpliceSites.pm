package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Nilsson::TbSpliceSites;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#8B4513', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["bf long slender","bf Short Stumpy","bf Lister 427","procyclic form","pf Alba 1 non-induced","pf Alba 1 induced","pf Alba 3 and 4 non-induced","pf Alba 3 and 4 induced"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['T.brucei RNASeq Spliced Leader And Poly A Sites from Nilsson uniqProfile','T.brucei RNASeq Spliced Leader And Poly A Sites from Nilsson nonUniqProfile'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
#      pct => {profiles => [''],
#              y_axis_label => 'Percentile',
#              x_axis_labels => $xAxisLabels,
#              default_y_max => 50,
#              colors => [$colors->[0]],
#              force_x_axis_label_horizontal => 1,
#             },
     });

  return $self;
}



1;

