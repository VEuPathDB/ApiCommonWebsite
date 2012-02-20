package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Hehl::Stress1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(6);

  $self->setProfileSetsHash
    ({'expr_val' => {profiles => ['Stress Response in Giardia lamblia Trophozoites-Averaged'],
                           y_axis_label => 'Expression Value',
                           colors => ['darkgreen'],
                           make_y_axis_fold_incuction => 1,
                           default_y_max => 1,
                           default_y_min => -1,
                          },
      pct => {profiles => ['Stress Response percentiles by temperature or varying DTT concentrations-red values',
                           'Stress Response percentiles by temperature or varying DTT concentrations-green values'
                          ],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors =>  ['grey', '#191970'],
              x_axis_labels => ['Control', '40 degrees C', '7.1 mM DTT', '14.2 mM DTT', '21.3 mM DTT'],
             },
     });

  return $self;
}



1;
