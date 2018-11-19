package ApiCommonWebsite::View::GraphPackage::EuPathDB::Beverley::LmThreeDevStages;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(5);
  $self->setLegendSize(40);

  my $colors =['#D87093', '#D87093', '#D87093', '#98FB98', '#98FB98', '#98FB98'];

  my $legend = ["Mouse Lesion Derived Amastigote vs. Early Log Procyclic Promastigote", "PNA - Metacyclic Promastigote vs. Early Log Procyclic Promastigote"];

  $self->setMainLegend({colors => ['#D87093', '#98FB98'], short_names => $legend, cols => 1});

  $self->setProfileSetsHash
    ({'expr_val' => {profiles => ['Profiles of L.major Beverly Steve array data'],
                           y_axis_label => 'Expression Value',
                           colors => $colors,
                           r_adjust_profile => 'profile = cbind(profile[1,1:3], profile[1,4:6]);',
                           default_y_max => 1.5,
                           default_y_min => -1.5,
                           force_x_axis_label_horizontal => 1,
                           make_y_axis_fold_incuction => 1,
                           x_axis_labels => ['A v P', 'M v P'],
                           plot_title => 'Transcript profiling of 3 L.major developmental stages',
                          },
      pct => {profiles => ['Percents of L.major Beverly Steve array data(red)',
                           'Percents of L.major Beverly Steve array data(green)'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              #r_adjust_profile => 'profile = cbind(profile[1,1:3], profile[1,4:6]);',
              colors => ['#D87093','#191970', '#D87093', '#191970', '#D87093','#191970', '#98FB98','#191970', '#98FB98', '#191970','#98FB98','#191970'],
              #x_axis_labels => ['MLDA vs ELPP', 'PNAMP vs ELPP'],
              x_axis_labels => ['AvP_1','AvP_2', 'AvP_3', 'MvP_1', 'MvP_2', 'MvP_3'],
              plot_title => 'Transcript profiling of 3 L.major developmental stages Percentiles',
             },
     });

  return $self;
}



1;
