package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Ringqvist::WbClone;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet);

use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(5);

  my $colors = ['#B22222', '#6A5ACD', '#87CEEB' ];
  my $legend =  ['DMEM', 'TYDK', 'Caco'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});


  $self->setProfileSetsHash
    ({expr_val => {profiles => ['Profiles of G.lamblia Ringqvist array data'],
              colors => $colors,
              x_axis_labels => ['1.5', '6', '18'],
              y_axis_label => 'Expression Value',
              make_y_axis_fold_incuction => 1,
	      r_adjust_profile => 'profile = rbind( c(profile[1,1], 0,0), profile[1,2:4], profile[1,5:7]);',
              force_x_axis_label_horizontal => 1,
              plot_title => 'Transcriptional changes in Giardia during host-parasite interactions ',
	      default_y_max => 0.2,
              default_y_min => -0.2,
             },

      pct => {profiles => ['Percents of G.lamblia Ringqvist array data(red)',
			   'Percents of G.lamblia Ringqvist array data(green)'],
              colors =>  ['#B22222', '#191970', '#6A5ACD', '#191970', '#6A5ACD', '#191970', '#6A5ACD', '#191970', '#87CEEB','#191970', '#87CEEB','#191970', '#87CEEB','#191970'],
              x_axis_labels => ['DMEM', 'TYDK 1.5', 'TYDK 6', 'TYDK 18', 'Caco 1.5', 'Caco 6', 'Caco 18'],
              y_axis_label => 'percentile',
              plot_title => 'Transcriptional changes in Giardia during host-parasite interactions ',
             }
     });

  return $self;
}



1;
