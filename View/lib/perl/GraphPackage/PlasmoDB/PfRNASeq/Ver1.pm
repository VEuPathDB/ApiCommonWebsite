package ApiCommonWebsite::View::GraphPackage::PlasmoDB::PfRNASeq::Ver1;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlotSet );
use ApiCommonWebsite::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#  $self->setScreenSize(250);
#  $self->setBottomMarginSize(8);
#  $self->setLegendSize(60);

  my $colors =['#000080'];

  $self->setProfileSetsHash
    ({coverage => {profiles => ['Profiles of P.falciparum Newbold mRNA Seq data'],
                   y_axis_label => 'Normalized Coverage (log2)',
                   x_axis_label => 'Hours',
                   default_y_max => 15,
                   default_y_min => 0,
                   r_adjust_profile => 'profile[profile < 1] = 1; profile = log2(profile); ',
                   colors => $colors,
                  },
      pct => {profiles => ['Percents of P. falciparum Newbold mRNA Seq data'],
              y_axis_label => 'Percentile',
              x_axis_label => 'Hours',
              default_y_max => 50,
              default_y_min => 0,
              colors => $colors,
             },
     });

  return $self;
}

1;

