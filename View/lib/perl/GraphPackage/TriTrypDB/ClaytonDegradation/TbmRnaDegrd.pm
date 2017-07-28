package ApiCommonWebsite::View::GraphPackage::TriTrypDB::ClaytonDegradation::TbmRnaDegrd;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  $self->setScreenSize(300);
  $self->setBottomMarginSize(10);

  my $colors =['#D87093', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["WT polyA Tet 0 min", "WT polyA Tet 30 min", "WT polyA NoTet 0 min", "WT polyA NoTet 30 min", "WT ribob- Tet 0 min","WT ribo- Tet 30 min","WT ribo- NoTet 0 min","WT ribo- NoTet 30 min","XRNA ribo- Tet 0 min","XRNA ribo- Tet 30 min","XRNA ribo- NoTet 0 min","XRNA ribo- NoTet 30 min" ];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rpkm => {profiles => ['T.brucei Clayton RNA Sequence of transcriptome-wide mRNA degradation minProfiles','T.brucei Clayton RNA Sequence of transcriptome-wide mRNA degradation diffProfiles'],
                   y_axis_label => 'log 2 (RPKM)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['T.brucei Clayton RNA Sequence of transcriptome-wide mRNA degradation minProfiles Percentile'],
              y_axis_label => 'Percentile',
              x_axis_labels => $xAxisLabels,
              default_y_max => 50,
              colors => [$colors->[0]],
             },
     });

  return $self;
}



1;
