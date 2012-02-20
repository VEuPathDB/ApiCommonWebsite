package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Hehl::Encystation;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(4);

  my $xAxisLabels = ['Control', '45 minute', '3 hour', '7 hour', '7 hour LS'];

  $self->setProfileSetsHash
    ({'expr_val' => {profiles => ['Hehl encystation expression profile'],
                     y_axis_label => 'Expression Value',
                     colors => ['darkgreen'],
                     make_y_axis_fold_incuction => 1,
                           default_y_max => 1,
                           default_y_min => -1,
                     x_axis_labels => $xAxisLabels,
                          },
      pct => {profiles => ['Hehl encystation expression percentile-red',
                           'Hehl encystation expression percentile-green'
                          ],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors =>  ['darkgreen', '#0066CC'],
              x_axis_labels => $xAxisLabels,
             },
     });

  return $self;
}



1;
