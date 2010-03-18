package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Parsons::TbDevelopmentalStages;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(180);
  $self->setBottomMarginSize(4);

  my $colors =['#B22222', '#1E90FF', '#00BFFF', '#FFD700', '#DAA520'];

  my $legend = ["Cultured BF", "Slender BF", "Stumpy BF", "log phase PF", "staionary phase PF"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rma => {profiles => ['Expression profiling of Tbrucei five life cycle stages'],
              y_axis_label => 'RMA Value (log2)',
              colors => $colors,
              r_adjust_profile => 'profile = log2(profile);',
              plot_title => 'Developmental stages of T.brucei - Variation in transcript abundance',
             },
      pct => {profiles => ['Percents of Tbrucei five life cycle stages'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors => $colors,
              r_adjust_profile => 'profile = profile * 100;',
              plot_title => 'Developmental stages of T.brucei - Percentile',
             },
     });

  return $self;
}



1;
