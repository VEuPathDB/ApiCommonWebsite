package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::Combined;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;



sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['purple', 'darkred', 'green', 'orange']; # as in the paper!
  my $pch = [19,24,20,23];
  my $legend = ['Ring', 'Trophozoite', 'Schizont', 'Late schiz.'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch});

  my @profileArray = (['Profiles of Derisi HalfLife-half_life', '', ''],
                     );


  my $id = $self->getId();

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $hl = EbrcWebsiteCommon::View::GraphPackage::BarPlot->new(@_);
  $hl->setProfileSets($profileSets);
  $hl->setColors($colors);
  $hl->setForceHorizontalXAxis(1);
  $hl->setPartName('half_life');
  $hl->setHighlightMissingValues(1);
  $hl->setYaxisLabel('half-life (min)');
  $hl->setPlotTitle("Half-life - $id");


  my @profileArrayLine = (['Profiles of Derisi HalfLife-Ring', '', ''],
                          ['Profiles of Derisi HalfLife-Trophozoite', '', ''],
                          ['Profiles of Derisi HalfLife-Schizont', '', ''],
                          ['Profiles of Derisi HalfLife-Late_Schizont', '', '']
                         );
  
  my $profileSetsLine = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayLine);

  my $line = EbrcWebsiteCommon::View::GraphPackage::LinePlot->new(@_);
  $line->setPartName('expr_val');
  $line->setProfileSets($profileSetsLine);
  $line->setColors($colors);
  $line->setYaxisLabel('half-life (min)');
  $line->setPlotTitle("Expression Normalized to 0 Hour - $id");
  $line->setPointsPch($pch);
  $line->setDefaultYMax(1);
  $line->setDefaultYMin(0);

  # R code normalizes to the 0HR Timepoint then filters away the Control Sample
  # Could have done the filtering by passing an array to "makeProfileSets" foreach of the profiles
  $line->setAdjustProfile("for(i in 1:nrow(lines.df)) { lines.df[i,] = 2^lines.df[i,]/2^lines.df[i,2]};lines.df = lines.df[,2:ncol(lines.df)];points.df = points.df[,2:ncol(points.df)];"); 

  $self->setGraphObjects($hl, $line);
  return $self;
}


1;

