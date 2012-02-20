package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Myler::LmSpliceSites;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#8B4513', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];
  my $xAxisLabels = ["L.m NSR","L.m Random"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({coverage => {profiles => ['Lmajor RNASeq Spliced Leader And Poly A Sites from Myler uniqProfile','Lmajor RNASeq Spliced Leader And Poly A Sites from Myler nonUniqProfile'],
                   y_axis_label => 'log 2 (normalized tag count)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   force_x_axis_label_horizontal => 1,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },
      pct => {profiles => ['Lmajor RNASeq Spliced Leader And Poly A Sites from Myler percentile'],
              y_axis_label => 'Percentile',
              x_axis_labels => $xAxisLabels,
              default_y_max => 50,
              colors => [$colors->[0]],
              force_x_axis_label_horizontal => 1,
             },
     });

  return $self;
}



1;

