package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Cross::TbRnaSeqBfPf;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#D87093', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["procyclic form", "blood form"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rpkm => {profiles => ['T.brucei George Cross RNA Sequence min-Profiles','T.brucei George Cross RNA Sequence diff-Profiles'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['T.brucei George Cross RNA Sequence min-Profiles Percentile'],
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

