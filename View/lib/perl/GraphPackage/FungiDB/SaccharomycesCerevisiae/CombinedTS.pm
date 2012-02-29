package ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::CombinedTS;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;


use ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrSingleBar;
use ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrSingleTS;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  # Copied from the big graph
  my $colors = ['blue', '#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];
  my $pch = [15,24,20,23,25];

  my $legend = ['Cln/Clb', 'pheromone', 'elutriation', 'cdc15', 'Cho et al'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch, , cols => 3});

  my $bar = ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrSingleBar->new();
  my $line = ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrSingleTS->new();

  $self->setGraphObjects($bar, $line);

  return $self;
}


1;
