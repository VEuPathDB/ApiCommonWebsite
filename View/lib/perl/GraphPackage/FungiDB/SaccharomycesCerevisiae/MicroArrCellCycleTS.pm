package ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrCellCycleTS;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;


use ApiCommonWebsite::View::GraphPackage::LinePlot;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['blue', '#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];
  my $pch = [15,24,20,23,25];

  my $legend = ['Cln/Clb', 'pheromone', 'elutriation', 'cdc15', 'Cho et al'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch, , cols => 3});


  my $clnClbProfileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['Expression profiling of saccharomyces cerevisiae s288c Cln/Clb experiments']]);

  my $clnClbPlot = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $clnClbPlot->setProfileSets($clnClbProfileSets);
  $clnClbPlot->setPartName('Cln_Clb');
  $clnClbPlot->setForceHorizontalXAxis(1);


  my $pheromoneProfileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['Expression profiling of saccharomyces cerevisiae s288c pheromone experiments']]);
  my $pheromonePlot = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $pheromonePlot->setProfileSets($pheromoneProfileSets);
  $pheromonePlot->setPartName('pheromone');

  $self->setGraphObjects($clnClbPlot, $pheromonePlot);

  return $self;
}

1;
