package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Myler::LdSpliceSites;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#8B4513'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["L.d NSR"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rpkm => {profiles => ['RNASeq Spliced Leader Sites from Myler uniqProfile - ldonBPK282A1','RNASeq Spliced Leader Sites from Myler nonUniqProfile - ldonBPK282A1'],
                   y_axis_label => 'log 2 (normalized tag count)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['RNASeq Spliced Leader Sites from Myler percentile - ldonBPK282A1'],
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

