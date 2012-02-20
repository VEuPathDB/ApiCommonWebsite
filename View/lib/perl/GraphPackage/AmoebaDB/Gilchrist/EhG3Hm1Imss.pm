package ApiCommonWebsite::View::GraphPackage::AmoebaDB::Gilchrist::EhG3Hm1Imss;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(3);

  my $colors = ['#FFCC33', '#006699',];

  my $legend = ['G3', 'HM1:IMSS',];

  $self->setMainLegend({colors => ['#FFCC33', '#006699',], short_names => $legend, cols=> 2});

    $self->setProfileSetsHash
    ({rma => {profiles => ['Ehist_Gilchrist_G3_V_HM1:IMSS'],
              stdev_profiles => ['standard error - Ehist_Gilchrist_G3_V_HM1:IMSS'],
               #  x_axis_labels => [],
               y_axis_label => 'RMA Value (log2)',
               colors => $colors,
             },
      pct => {profiles => ['percentile - Ehist_Gilchrist_G3_V_HM1:IMSS'],
              # x_axis_labels => [],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}
1;
