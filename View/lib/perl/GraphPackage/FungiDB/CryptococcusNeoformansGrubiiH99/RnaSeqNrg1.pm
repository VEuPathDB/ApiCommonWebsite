package ApiCommonWebsite::View::GraphPackage::FungiDB::CryptococcusNeoformansGrubiiH99::RnaSeqNrg1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#D87093', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['C.neoformans NRG1 Expression', 
                                'C.neoformans NRG1 Expression-diff profiles'
                               ],
                   y_axis_label => 'log 2 (RPKM)',
                   colors => $colors,
                  default_y_max => 4,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                   force_x_axis_label_horizontal => 1,
                   x_axis_labels => ['H99 Wildtype','nrg1 KO','nrg1 Over-expression']
                  },
      pct => {profiles => ['percentile - C.neoformans NRG1 Expression'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
                   force_x_axis_label_horizontal => 1,
              colors => [$colors->[0]],
             },
     });




  return $self;
}



1;
