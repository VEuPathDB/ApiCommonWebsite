package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Tschudi::TbRNATrscriptomeMapping;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  $self->setScreenSize(300); 
  $self->setBottomMarginSize(10);

  my $colors =['#D87093', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["3'-end-enriched oligo(dT) primed cDNA", "3'-end-enriched random primed cDNA", "5'-SL-end-enriched cDNA", "5'-triphosphate-end-enriched cDNA"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rpkm => {profiles => ['T.brucei Tschudi RNA Seq data','T.brucei Tschudi RNA Seq data-diff'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['percentile - T.brucei Tschudi RNA Seq data'],
              y_axis_label => 'Percentile',
              x_axis_labels => $xAxisLabels,
              default_y_max => 50,
              colors => [$colors->[0]],
             },
     });

  return $self;
}



1;
