package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Duffy::PfRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(4);
  $self->setLegendSize(10);

  my $colors =['#E9967A', '#66CDAA', '#8B4513'];
  my $legend = ["PL01:Pregnant Women", "PL02:Children", "3D7"];

  $self->setMainLegend({colors => ['#E9967A', '#66CDAA', '#8B4513'], short_names => $legend, cols => 3});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['P.falciparum duffy mRNA Seq data'],
                   y_axis_label => 'Normalized Coverage (log2)',
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile[profile < 1] = 1; profile = log2(profile); ',
                   x_axis_labels => $legend,
                  },
      pct => {profiles => ['percentile - P.falciparum duffy mRNA Seq data'],
              y_axis_label => 'Percentile',
              default_y_max => 100,
              colors => $colors,
              force_x_axis_label_horizontal => 1,
              x_axis_labels => $legend,
             },
     });

  return $self;
}



1;
