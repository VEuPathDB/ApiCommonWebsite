package ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroArrCellCycleTS;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;


use EbrcWebsiteCommon::View::GraphPackage::LinePlot;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['blue', '#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];
  my $pch = [15,24,20,23,25];

  my $legend = ['Cln/Clb', 'pheromone', 'elutriation', 'cdc15', 'Cho et al'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch, , cols => 3});


  my $clnClbProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Expression profiling of saccharomyces cerevisiae s288c Cln/Clb experiments']]);

  my $clnClbPlot = EbrcWebsiteCommon::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $clnClbPlot->setProfileSets($clnClbProfileSets);
  $clnClbPlot->setPartName('Cln_Clb');
  $clnClbPlot->setForceHorizontalXAxis(1);


  my $pheromoneProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Expression profiling of saccharomyces cerevisiae s288c pheromone experiments']]);
  my $pheromonePlot = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $pheromonePlot->setProfileSets($pheromoneProfileSets);
  $pheromonePlot->setPartName('pheromone');

  my $elutriationProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Expression profiling of saccharomyces cerevisiae s288c elutriation experiments']]);
  my $elutriationPlot = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $elutriationPlot->setProfileSets($elutriationProfileSets);
  $elutriationPlot->setPartName('elutriation');

  my $cdc15ProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Expression profiling of saccharomyces cerevisiae s288c cdc15 Experiments']]);
  my $cdc15Plot = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $cdc15Plot->setProfileSets($cdc15ProfileSets);
  $cdc15Plot->setPartName('cdc15');

  my $cdc28ProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Expression profiling of saccharomyces cerevisiae s288c microarray from Cho et al.']]);
  my $cdc28Plot = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $cdc28Plot->setProfileSets($cdc28ProfileSets);
  $cdc28Plot->setPartName('cdc28');

  $self->setGraphObjects($clnClbPlot, $pheromonePlot, $elutriationPlot, $cdc15Plot, $cdc28Plot);

  return $self;
}

1;
