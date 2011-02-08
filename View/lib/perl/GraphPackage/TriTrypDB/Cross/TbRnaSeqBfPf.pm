package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Cross::TbRnaSeqBfPf;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  $self->setScreenSize(180);
#  $self->setBottomMarginSize(4);

  my $colors =['#D87093','#66CDAA'];

  my $legend = ["procyclic form", "blood form"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['T.brucei George Cross RNA Sequence min-Profiles'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $legend,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);stdev=stdev + 1; stdev = log2(stdev);',
                   stdev_profiles => ['T.brucei George Cross RNA Sequence diff-Profiles'],
                  },
      pct => {profiles => ['T.brucei George Cross RNA Sequence min-Profiles Percentile'],
              y_axis_label => 'Percentile',
              x_axis_labels => $legend,
              default_y_max => 50,
              colors => $colors,
              force_x_axis_label_horizontal => 1,
              stdev_profiles => ['T.brucei George Cross RNA Sequence diff-Profiles Percentile'],
             },
     });

  return $self;
}



1;
