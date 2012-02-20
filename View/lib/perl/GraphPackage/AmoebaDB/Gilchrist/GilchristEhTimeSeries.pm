package ApiCommonWebsite::View::GraphPackage::AmoebaDB::Gilchrist::GilchristEhTimeSeries;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setBottomMarginSize(9);
  $self->setLegendSize(60);

  my $colors =['#800517', '#307D7E','#254117', '#7E3517', '#806517'];

  my $xAxisLabels = ['HM-1:IMSS', 'Rahman','HM-1:IMSS-MA', 'HM-1:IMSS-MA 1d PI','HM-1:IMSS-MA 29d PI'];

  my $legend = ["HM-1:IMSS - Trophs TYI", "Rahman Trophs TYI","HM1:IMSS mouse-adapted-Trophs TYI", "HM1:IMSS mouse-adapted - Trophs 1d PI","HM1:IMSS mouse-adapted - Trophs 29d PI"];

   $self->setMainLegend({colors => $colors, short_names => $legend,cols => 1});

  $self->setProfileSetsHash
    ({'rma' => {profiles => ['EhistolyticaNugenProfiles'],
                           y_axis_label => 'RMA Value (log2)',
                           colors => $colors,
                           default_y_max => 15,
                           plot_title => 'E. histolytica Stage Conversion Nugen array Profiles',
                           x_axis_labels => $xAxisLabels,
                          },
      pct => {profiles => ['EhistolyticaNugenProfilePcts'
                          ],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors =>  ['#800517', '#307D7E','#254117', '#7E3517', '#806517'],
              plot_title => 'E. histolytica Stage Conversion Nugen array Profiles',
              x_axis_labels => $xAxisLabels,
             },
     });

  return $self;
}



1;
