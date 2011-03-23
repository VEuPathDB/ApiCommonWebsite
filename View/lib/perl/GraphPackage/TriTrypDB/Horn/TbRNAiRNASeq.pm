package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Horn::TbRNAiRNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#B8860B', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];

  my $xAxisLabels = ['No_Tet',
                     'BFD3',
                     'BFD6',
                     'PF',
                     'DIF'];

  $self->setMainLegend({colors => $colors, short_names => $legend});





  $self->setProfileSetsHash
    ({coverage => {profiles => ['T.brucei Horn RNAi Sequence minProfiles', 'T.brucei Horn RNAi Sequence diffProfiles'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
     });

  return $self;
}



1;
