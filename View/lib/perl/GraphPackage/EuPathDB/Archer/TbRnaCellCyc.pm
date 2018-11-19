package ApiCommonWebsite::View::GraphPackage::EuPathDB::Archer::TbRnaCellCyc;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#D87093', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["G1 (0.5 Hrs)", "G1 (3 Hrs)", "S (5.5 Hrs)", "G2 (7.25 Hrs)"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rpkm => {profiles => ['T.brucei Archer Stuart Cell Cycle RNA Sequence min-Profiles','T.brucei Archer Stuart Cell Cycle RNA Sequence diff-Profiles'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['T.brucei Archer Stuart Cell Cycle RNA Sequence min-Profiles Percentile'],
              y_axis_label => 'Percentile',
              x_axis_labels => $xAxisLabels,
              default_y_max => 50,
              colors => [$colors->[0]],
              force_x_axis_label_horizontal => 1,
             },
     });

  return $self;
}



1;
