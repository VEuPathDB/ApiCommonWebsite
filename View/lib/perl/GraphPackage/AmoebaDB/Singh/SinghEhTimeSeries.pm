package ApiCommonWebsite::View::GraphPackage::AmoebaDB::Singh::SinghEhTimeSeries;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setBottomMarginSize(8);
  $self->setLegendSize(60);

  my $colors =['#E9967A', '#8B4513','#66CDAA', '#556B2F', '#87CEEB','#008080', '#C9BE62'];

  my $xAxisLabels = ['HM-1:IMSS', 'Rahman','200:NIH', '200:NIH LG','MS75-3544 1wk','MS75-3544 8wk','2592100 3wk'];

  my $legend = ["HM-1:IMSS Trophs TYI", "Rahman Trophs TYI","200:NIH Trophs TYI", "200:NIH Trophs TYI Low Glucose","Trophs/Cysts 1wk Robinsons","Trophs/Cysts 8wk Robinsons","Trophs/Cysts 3wk Robinsons"];

   $self->setMainLegend({colors => $colors, short_names => $legend,cols => 2});

  $self->setProfileSetsHash
    ({'rma' => {profiles => ['EhistolyticaAffyProfiles'],
                           y_axis_label => 'RMA Value (log2)',
                           default_y_max => 15,
                           colors => $colors,
                           x_axis_labels => $xAxisLabels,
                          },
      pct => {profiles => ['EhistolyticaAffyProfilePcts'
                          ],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors =>  ['#E9967A', '#8B4513','#66CDAA', '#556B2F', '#87CEEB','#008080', '#C9BE62'],
              x_axis_labels => $xAxisLabels,
             },
     });

  return $self;
}

1;
