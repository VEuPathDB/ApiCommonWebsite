package ApiCommonWebsite::View::GraphPackage::EuPathDB::Matthews::TbDifferentiationSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::LinePlotSet );
use EbrcWebsiteCommon::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#CD853F'];

  my $pch = [15];

  $self->setProfileSetsHash
    ({expr_val => {profiles => ['Expression profiling of T. brucei differentiation series',
                                     ],
                         y_axis_label => 'Expression Value',
                         x_axis_label => ' ',
                         colors => $colors,
                         plot_title => 'Expression profiling of T. brucei differentiation series',
                         default_y_max => 2,
                         default_y_min => -2,
                         default_x_min => 0,
                         r_adjust_profile => 'profile = log2(2^profile/2^profile[2]);',
                         make_y_axis_fold_incuction =>  1,
                         points_pch => $pch,
                        },
      pct => {profiles => ['Percentiles of T. brucei differentiation series',
                          ],
              y_axis_label => 'Percentile',
              x_axis_label => ' ',
              colors => $colors,
              plot_title => 'Percentiles of T. brucei differentiation series',
              default_y_max => 10,
              default_y_min => 4,
              default_x_min => 0,
              points_pch => $pch,
             },

     });

  return $self;
}



1;
