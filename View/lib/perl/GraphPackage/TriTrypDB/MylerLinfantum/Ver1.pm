package ApiCommonWebsite::View::GraphPackage::TriTrypDB::MylerLinfantum::Ver1;
use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::LinePlotSet );
use EbrcWebsiteCommon::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#990099','#009999'];

  my $pch = [19,22];

  my $legend = ['Replicate 1', 'Replicate 2'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({'expr_val' => {profiles => ['Linfantum promastigote time-course biorep01',
                                  'Linfantum promastigote time-course biorep02'
                                 ],
                           y_axis_label => 'Expression Value',
                           x_axis_label => 'Hours',
                           colors => $colors,
                           make_y_axis_fold_incuction => 1,
                           default_y_max => 2,
                           default_y_min => -2,
                           default_x_min => 0,
                           points_pch => $pch,
                     r_adjust_profile => 'profile = log2(2^profile/2^profile[1]);',
                          },
      'pct' => {profiles => ['Percents of the Linfantum promastigote time-course biorep01',
                             'Percents of the Linfantum promastigote time-course biorep02'
                            ],
                y_axis_label => 'percent',
                           x_axis_label => 'Hours',
                colors => $colors,
                default_y_max => 60,
                           default_y_min => 0,
                default_x_min => 0,
                points_pch => $pch,
               },
      
      
     });

  return $self;
}



1;





