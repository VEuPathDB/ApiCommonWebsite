package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Cowman::Sir2KO;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(200);
  $self->setBottomMarginSize(6);

  my $colors = ['#6495ED', '#E9967A', '#2F4F4F' ];

  $self->setProfileSetsHash
    ({rma => {profiles => ['Profiles of E-TABM-438 from Cowman'],
              y_axis_label => 'RMA Value (log2)',
              x_axis_labels => ['ring', 'schizont', 'trophozoite'],
              colors => $colors,
              plot_title => 'Transcription profiling of WT, Pfsir2A and Pfsir2B KO intra-erythrocytic stages',
              r_adjust_profile => 'profile = rbind(profile[1,1:3], profile[1,4:6], profile[1,7:9]);',
              legend => ['Wild Type', 'sir2A KO', 'sir2B KO'],
             },
      pct => {profiles => ['Percents of E-TABM-438 from Cowman'],
              y_axis_label => 'percentile',
              x_axis_labels => ['ring', 'schizont', 'trophozoite'],
              colors => $colors,
              plot_title => 'Percentiles of WT, Pfsir2A and Pfsir2B KO intra-erythrocytic stages',
              r_adjust_profile => 'profile = profile * 100;profile = rbind(profile[1,1:3], profile[1,4:6], profile[1,7:9]);',
              legend => ['Wild Type', 'sir2A KO', 'sir2B KO'],
             },
      });

  return $self;
}



#Percents of E-TABM-438 from Cowman


1;
