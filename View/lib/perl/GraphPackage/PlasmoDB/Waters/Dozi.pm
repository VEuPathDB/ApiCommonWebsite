package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Waters::Dozi;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(4);

  my $colors = ['#B22222' ];

  $self->setProfileSetsHash
    ({rma => {profiles => ['Profiles of P. berghei DOZI array data'],
              y_axis_label => 'log2(R/G)',
              colors => $colors,
              x_axis_labels => ['KO vs. WT'],
              plot_title => 'DOZI Knock Out vs. Wild Type',
              default_y_max => 2,
              default_y_min => -2,
              force_x_axis_label_horizontal => 1,
              make_y_axis_fold_incuction => 1,
             },
     });

  return $self;
}



1;
