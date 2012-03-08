package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Nilsson::TbSpliceSites;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  $self->setScreenSize(280);
  $self->setBottomMarginSize(10);

  my $colors =['#8B4513', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["long slender","short stumpy","Lister 427","procyclic","Alba 1 - ","Alba 1 + ","Alba 3_4 - ","Alba 3_4 + "];
#  my $xAxisLabels = ["bf long slender","bf short stumpy","bf Lister 427","procyclic form","pf Alba 1 N","pf Alba 1 I","pf Alba 3/4 N","pf Alba 3/4 I"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['T.brucei RNASeq Spliced Leader And Poly A Sites from Nilsson uniqProfile','T.brucei RNASeq Spliced Leader And Poly A Sites from Nilsson nonUniqProfile'],
                   y_axis_label => 'log 2 (normalized tag count)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['T.brucei RNASeq Spliced Leader And Poly A Sites from Nilsson percentile'],
              y_axis_label => 'Percentile',
              x_axis_labels => $xAxisLabels,
              default_y_max => 50,
              colors => [$colors->[0]],
             },
     });

  return $self;
}



1;

