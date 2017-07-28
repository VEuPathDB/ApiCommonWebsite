package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Estevez::Drbd3Depletion;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BarPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(4);
  $self->setLegendSize(40);

  my $colors =['#D8BFD8'];

  $self->setProfileSetsHash
    ({'expr_val' => {profiles => ['Expression profiling of Tbrucei Procyclic TbDRBD3 depletion'],
                           y_axis_label => 'Expression Value',
                           colors => $colors,
                           make_y_axis_fold_incuction => 1,
                           default_y_max => 1.5,
                           default_y_min => -1.5,
                           force_x_axis_label_horizontal => 1,
                           plot_title => 'Effect of TbDRBD3 depletion on procyclic T. brucei transcriptome',
                          },
     });

  return $self;
}



1;
