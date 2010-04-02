package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Duffy::PfRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(4);
  $self->setLegendSize(10);

  my $colors =['#E9967A', '#66CDAA', '#8B4513'];
  my $legend = ["3D7", "PL01", "PL02"];

  $self->setMainLegend({colors => ['#D87093', '#98FB98'], short_names => $legend, cols => 3});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['Profiles of P.falciparum duffy mRNA Seq data'],
                   y_axis_label => 'Normalized Coverage',
                   colors => $colors,
                   #force_x_axis_label_horizontal => 1,
                   x_axis_labels => ['3D7', 'PL01', 'PL02'],
		   plot_title => 'P.falciparum RNA Sequence Coverage',
                  },
      pct => {profiles => ['Percents of P.falciparum duffy mRNA Seq data'],
              y_axis_label => 'Percentile',
              default_y_max => 100,
              r_adjust_profile => 'profile = profile * 100;',
              colors => $colors,
              #force_x_axis_label_horizontal => 1,
              x_axis_labels => ['3D7', 'PL01', 'PL02'],
              plot_title => 'P.falciparum RNA Sequence Coverage',
             },
     });

  return $self;
}



1;
