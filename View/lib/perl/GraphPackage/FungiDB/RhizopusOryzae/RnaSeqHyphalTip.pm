package ApiCommonWebsite::View::GraphPackage::FungiDB::RhizopusOryzae::RnaSeqHyphalTip;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#D87093', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
#  my $xAxisLabels = ["procyclic form", ""];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['rhizopus_oryzae_99880 hyphal_tip RNA Sequence min Profiles', 
                                'rhizopus_oryzae_99880 hyphal_tip RNA Sequence diff Profiles'
                               ],
                   y_axis_label => 'log 2 (RPKM)',
                   colors => $colors,
                  default_y_max => 4,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                   x_axis_labels => ['RO3H','RO5H','RO20H']
                  },
      pct => {profiles => ['rhizopus_oryzae_99880 hyphal_tip RNA Sequence min Profiles Percentile'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors => [$colors->[0]],
             },
     });

  return $self;
}



1;
