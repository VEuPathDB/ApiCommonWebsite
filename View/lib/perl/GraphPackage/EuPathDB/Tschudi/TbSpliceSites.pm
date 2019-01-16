package ApiCommonWebsite::View::GraphPackage::EuPathDB::Tschudi::TbSpliceSites;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  $self->setScreenSize(280);
  $self->setBottomMarginSize(10);

  my $colors =['#8B4513'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["5-SL-end-enriched cDNA"];


  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rpkm => {profiles => ['RNASeq Spliced Leader Sites from Tschudi uniqProfile - tbruTREU927', 'RNASeq Spliced Leader Sites from Tschudi nonUniqProfile - tbruTREU927'],
                   y_axis_label => 'log 2 (normalized tag count)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['RNASeq Spliced Leader Sites from Tschudi percentile - tbruTREU927'],
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

