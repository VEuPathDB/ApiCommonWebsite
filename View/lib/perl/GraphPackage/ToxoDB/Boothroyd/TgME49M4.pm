package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TgME49M4;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(220);
	$self->setBottomMarginSize(6.5);

  my $colors = ['#ff0000', '#000000', '#000000', '#ADDFFF', '#0000ff' ,'#0000ff', '#0000ff'];
  my $legend = ["oocyst", "sporozoite", "tachyzoite", "bradyzoite"];

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
