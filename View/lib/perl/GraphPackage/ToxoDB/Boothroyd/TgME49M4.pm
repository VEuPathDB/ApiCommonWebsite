package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TgME49M4;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);

  my $colors = ['#ff0000', '#000000', '#000000', '#ADDFFF', '#0000ff' ,'#0000ff', '#0000ff'];
  my $legend = ["oocyst", "sporozoite", "tachyzoite", "bradyzoite"];

  #my $xAxisLabels = ["unsporulated", "4 days sporulated", "10 days sporulated", "2 days in vitro", "..."];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rma => {profiles => ['Expression profiles of Tgondii ME49 Boothroyd experiments'],
              y_axis_label => 'RMA Value (log2)',
              colors => $colors,
              plot_title => 'Tachyzoite comparison of archetypal T.gondii lineages',
             },
      pct => {profiles => ['Expression percentile profiles of Tgondii ME49 Boothroyd experiments'],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
