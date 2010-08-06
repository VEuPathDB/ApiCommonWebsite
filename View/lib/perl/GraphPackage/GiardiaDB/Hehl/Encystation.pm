package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Hehl::Encystation;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(4);

  my $xAxisLabels = ['Self', '45 min', '3 hr', '7 hr', '7 hr LS'];

  $self->setProfileSetsHash
    ({'expr_val' => {profiles => ['Hehl encystation expression profile'],
                     y_axis_label => 'Expression Value',
                     colors => ['darkgreen'],
                     make_y_axis_fold_incuction => 1,
                     default_y_max => 0.2,
                     default_y_min => -0.2,
                     x_axis_labels => $xAxisLabels,
                          },
      pct => {profiles => ['Hehl encystation expression percentile-red',
                           'Hehl encystation expression percentile-green'
                          ],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors =>  ['grey', '#191970'],
              x_axis_labels => $xAxisLabels,
             },
     });

  return $self;
}



1;
