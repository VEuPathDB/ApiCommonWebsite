package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Horn::TbRNAiRNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors0 =['#191970', '#DDDDDD'];
  my $colors1 =['#B8860B', '#DDDDDD'];

  my $legendColors = [$colors0->[0], @$colors1];
  my $legend = ["Uniquely Mapped - CDS", "Uniquely Mapped - Transcript", "Non-Uniquely Mapped"];

  my $xAxisLabels = ['No_Tet',
                     'BFD3',
                     'BFD6',
                     'PF',
                     'DIF'];

  $self->setMainLegend({colors => $legendColors, short_names => $legend});


  $self->setProfileSetsHash
    ({     cds_rpkm => {profiles => ['T.brucei Horn RNAi Sequence minProfiles using CDS coordinates', 'T.brucei Horn RNAi Sequence diffProfiles using CDS coordinates'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors0,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },

transcript_rpkm => {profiles => ['T.brucei Horn RNAi Sequence minProfiles', 'T.brucei Horn RNAi Sequence diffProfiles'],
                          y_axis_label => 'log 2 (RPKM)',
                          x_axis_labels => $xAxisLabels,
                          colors => $colors1,
                          force_x_axis_label_horizontal => 1,
                          r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                          stack_bars => 1,
                         },
     }); 

  return $self;
}



1;
