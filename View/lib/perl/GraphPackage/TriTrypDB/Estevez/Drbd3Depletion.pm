package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Estevez::Drbd3Depletion;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(4);
  $self->setLegendSize(40);

  my $colors =['#D8BFD8'];

  $self->setProfileSetsHash
    ({'m' => {profiles => ['Expression profiling of Tbrucei Procyclic TbDRBD3 depletion'],
              y_axis_label => 'log2(R/G)',
              colors => $colors,
              default_y_max => 1.5,
              default_y_min => -1.5,
              force_x_axis_label_horizontal => 1,
              plot_title => 'Effect of TbDRBD3 depletion on procyclic T. brucei transcriptome',
             },
      pct => {profiles => ['Percent of Expression profiling of Tbrucei Procyclic TbDRBD3 depletion'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors => $colors,
              force_x_axis_label_horizontal => 1,
              plot_title => 'Effect of TbDRBD3 depletion on procyclic T. brucei transcriptome',
             },
     });

  return $self;
}



1;
