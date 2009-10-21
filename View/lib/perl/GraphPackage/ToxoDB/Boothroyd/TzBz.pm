package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  $self->setProfileSetsHash
    ({rma => {profiles => ['expression profiles of T. gondii Matt_Tz-Bz time series'],
              y_axis_label => 'RMA Value (log2)',
              colors => ['#7F525D','#348781',  '#A0CFEC', '#AF7817'],
              plot_title => 'Tachyzoite to Bradyzoite Differentiation Time Series',
             },

      pct => {profiles => ['expression profile percentiles of T. gondii Matt_Tz-Bz time series'],
              y_axis_label => 'percentile',
              colors => ['#7F525D','#348781',  '#A0CFEC', '#AF7817'],
              plot_title => 'Tachyzoite to Bradyzoite Differentiation Time Series - Percentiles',
             }
     });

  return $self;
}



1;
