package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Cross::TbRnaSeqBfPf;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(180);
  $self->setBottomMarginSize(4);

  my $colors =['#66CDAA', '#D87093'];

  my $legend = ["blood form", "procyclic form"];

  $self->setMainLegend({colors => $colors, short_names => $legend});

  $self->setProfileSetsHash
    ({rma => {profiles => ['T.brucei George Cross RNA Sequence Profiles'],
              y_axis_label => 'normalized coverage',
              colors => $colors,
              r_adjust_profile => 'profile = log2(profile);',
              plot_title => 'T.brucei blood and procyclic forms RNA Sequence Coverage',
             },
      pct => {profiles => ['T.brucei George Cross RNA Sequence Profiles Percentile'],
              y_axis_label => 'Percentile',
              default_y_max => 50,
              colors => $colors,
              r_adjust_profile => 'profile = profile * 100;',
              plot_title => 'T.brucei blood and procyclic forms RNA Sequence Coverage',
             },
     });

  return $self;
}



1;
