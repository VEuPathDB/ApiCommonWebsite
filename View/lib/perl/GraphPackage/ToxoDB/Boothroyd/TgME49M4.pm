package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TgME49M4;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(6);


  my $colors = ['#FFC0CB', '#D87093', '#D87093', '#FFDAB9', '#AFEEEE' ,'#AFEEEE', '#AFEEEE'];

  my $legend = ["oocyst", "sporozoite", "tachyzoite", "bradyzoite"];

  $self->setMainLegend({colors => ['#FFC0CB', '#D87093', '#FFDAB9', '#AFEEEE'], short_names => $legend, cols=> 4});

  $self->setProfileSetsHash
    ({rma => {profiles => ['Expression profiles of Tgondii ME49 Boothroyd experiments'],
              y_axis_label => 'RMA Value (log2)',
              colors => $colors,
							x_axis_labels => ['d0 unsporulated', 'd4 sporulation', 'd10 sporulation', 'd2 in intro Tz', 'd4 in vitro Bz', 'd8 in vitro Bz', 'd21 IN VIVO Bz'],
              plot_title => 'Tachyzoite comparison of archetypal T.gondii lineages',
             },
      pct => {profiles => ['Expression percentile profiles of Tgondii ME49 Boothroyd experiments'],
							x_axis_labels => ['d0 unsporulated', 'd4 sporulation', 'd10 sporulation', 'd2 in intro Tz', 'd4 in vitro Bz', 'd8 in vitro Bz', 'd21 IN VIVO Bz'],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
