package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Stunnenberg::PfRBCRnaSeq;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setBottomMarginSize(8);
#  $self->setLegendSize(60);

  my $colors =['#E9967A', '#8B4513','#66CDAA', '#556B2F', '#87CEEB','#008080', '#F0E68C','#DAA520'];

  my $xAxisLabels = ['5 hours','10 hours','15 hours','20 hours','25 hours','30 hours','35 hours','40 hours'];

  $self->setProfileSetsHash
    ({coverage => {profiles => ['Profiles of P.falciparum Stunnenberg mRNA Seq data'],
                   y_axis_label => 'Normalized Coverage',
                   default_y_max => 15,
#                   r_adjust_profile => 'for(i in length(profile)) {if(profile[i] < 1) {profile[i] = 1}}; profile = log2(profile); ',
                   colors => $colors,
                   plot_title => 'P. falciparum post infection RNA Seq Profiles',
                   x_axis_labels => $xAxisLabels,
                  },
      pct => {profiles => ['Percents of P. falciparum Stunnenberg mRNA Seq data'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors => $colors,
              plot_title => 'P. falciparum post infection RNA Seq Profiles',
              x_axis_labels => $xAxisLabels,
             },
     });

  return $self;
}

1;
