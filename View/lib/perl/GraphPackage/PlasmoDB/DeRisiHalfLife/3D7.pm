package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::3D7;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlotSet );
use ApiCommonWebsite::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['purple', 'darkred', 'green', 'orange']; # as in the paper!

  my $pch = [19,24,20,23];

  my $legend = ['Ring', 'Trophozoite', 'Schizont', 'Late schiz.'];


  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch});

  $self->setProfileSetsHash
    ({ expr_val => {profiles => ['Profiles of Derisi HalfLife-Ring',
				'Profiles of Derisi HalfLife-Trophozoite',
				'Profiles of Derisi HalfLife-Schizont',
				'Profiles of Derisi HalfLife-Late_Schizont',
                          ],
                   y_axis_label => 'expression Value (log2)',
                   x_axis_label => 'Minutes',
                   colors => $colors,
                   r_adjust_profile => 'profile = 2^profile/2^profile[2]; profile = profile[2:length(profile)]; element.names = element.names[2:length(element.names)]; ',
                   plot_title => '',
                   default_y_max => 1,
#              smooth_spline => 1,
#              spline_approx_n => 60,
                   points_pch => $pch,
                  },
     });


#  $self->setScreenSize(200);
#  $self->setBottomMarginSize(4);


  return $self;
}



1;
