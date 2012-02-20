package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TgME49M4;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setPlotWidth(450);
  $self->setBottomMarginSize(6);


  my $colors = ['#D87093', '#D87093', '#D87093', '#E9967A', '#87CEEB', '#87CEEB', '#87CEEB'];

  my $legend = ["oocyst", "tachyzoite", "bradyzoite"];

  $self->setMainLegend({colors => ['#D87093', '#E9967A', '#87CEEB'], short_names => $legend, cols=> 3});

  $self->setProfileSetsHash
    ({rma => {profiles => ['Expression profiles of Tgondii ME49 Boothroyd experiments'],
	      profile_display_names => ['Expression profiles'],
	      x_axis_labels => ['d0 unsporulated', 'd4 sporulation', 'd10 sporulation', 'd2 in vitro Tz', 'd4 in vitro Bz', 'd8 in vitro Bz', 'd21 IN VIVO Bz'],
              y_axis_label => 'RMA Value (log2)',
              colors => $colors,
              plot_title => 'oocyst/tz/bz development in the type II strain M4',
             },
      pct => {profiles => ['Expression percentile profiles of Tgondii ME49 Boothroyd experiments'],
	      profile_display_names => ['Expression percentile profiles'],
	      x_axis_labels => ['d0 unsporulated', 'd4 sporulation', 'd10 sporulation', 'd2 in vitro Tz', 'd4 in vitro Bz', 'd8 in vitro Bz', 'd21 IN VIVO Bz'],
              y_axis_label => 'Percentile',
              colors => $colors,
            }
     });

  return $self;
}

1;
