package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Nilsson::TcSpliceSites;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  $self->setScreenSize(280);
  $self->setBottomMarginSize(10);

  my $colors =['#8B4513', '#DDDDDD'];

  my $legend = ["Uniquely Mapped", "Non-Uniquely Mapped"];

  # same as in database - might need to be changed
#  my $xAxisLabels = ["ama wt","ama J1 ko","meta wt","meta J1 ko","epi wt","epi J1 ko","trypo wt","trypo J1 ko"];
  my $xAxisLabels = ["Amas. JBP1 ko","Amas. wild type","Epimas. JBP1 ko","Epimas. wild type","Metacyc. JBP1 ko","Metacyc. wild type","Trypo. JBP1 ko","Trypo. wild type"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rpkm => {profiles => ['Tcruzi RNASeq Spliced Leader And Poly A Sites from Nilsson uniqProfile','Tcruzi RNASeq Spliced Leader And Poly A Sites from Nilsson nonUniqProfile'],
                   y_axis_label => 'log 2 (normalized tag count)',
                   x_axis_labels => $xAxisLabels,
                   colors => $colors,
                   r_adjust_profile => 'profile=profile + 1; profile = log2(profile);',
                   stack_bars => 1,
                  },

      pct => {profiles => ['Tcruzi RNASeq Spliced Leader And Poly A Sites from Nilsson percentile'],
              y_axis_label => 'Percentile',
              x_axis_labels => $xAxisLabels,
              default_y_max => 50,
              colors => [$colors->[0]],
             },
     });

  return $self;
}



1;

