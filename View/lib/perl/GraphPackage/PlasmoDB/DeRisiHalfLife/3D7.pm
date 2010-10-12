package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::3D7;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['purple', 'darkred', 'green', 'orange']; # as in the paper!
  my $pch = [19,24,20,23];

  my $legend = ['Ring', 'Trophozoite', 'Schizont', 'Late schiz.'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['Profiles of Derisi HalfLife-Ring',
				'Profiles of Derisi HalfLife-Trophozoite',
				'Profiles of Derisi HalfLife-Schizont',
				'Profiles of Derisi HalfLife-Late_Schizont',
                          ],
              y_axis_label => 'expression Value (log2)',
              x_axis_label => 'Minutes',
              colors => $colors,
              plot_title => '',
              default_y_max => 1,
              default_y_min => 0, #-0.5,
              points_pch => $pch,
#              r_adjust_profile => 'profile = 2^profile',
             },
     });


#  $self->setScreenSize(200);
#  $self->setBottomMarginSize(4);


  return $self;
}



1;
