package ApiCommonWebsite::View::GraphPackage::FungiDB::NeurosporaCrassaOR74A::RnaSeqHyphalGrowth;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#D87093', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['NcraOR74A Hyphal Growth RNASeq', 
                                'NcraOR74A Hyphal Growth RNASeq - diff'
                               ],
                   y_axis_label => 'log 2 (RPKM)',
                   colors => $colors,
                  default_y_max => 4,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                   force_x_axis_label_horizontal => 1,
                   x_axis_labels => ['3 HR', '5 HR', '20 HR']


                  },
      pct => {profiles => ['percentile - NcraOR74A Hyphal Growth RNASeq'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
                   force_x_axis_label_horizontal => 1,
              colors => [$colors->[0]],
             },
     });

  return $self;
}



1;
