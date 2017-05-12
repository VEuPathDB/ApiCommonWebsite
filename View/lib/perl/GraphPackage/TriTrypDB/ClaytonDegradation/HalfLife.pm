package ApiCommonWebsite::View::GraphPackage::TriTrypDB::ClaytonDegradation::HalfLife;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;
use ApiCommonWebsite::View::GraphPackage::GGBarPlot;

use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24,15,17];
  my $colors =['#996622','#0049A8',];

  my $halfLifeSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([['mRNA decay in bloodstream and procyclic form - half_life','values', 'mRNA decay in bloodstream and procyclic form - half_life_error','values', undef,undef,undef,undef,'half-life']]);

  my $id = $self->getId();

  my $halfLife = ApiCommonWebsite::View::GraphPackage::GGBarPlot->new(@_);
  $halfLife->setProfileSets([$halfLifeSets->[0]]);
  $halfLife->setYaxisLabel('Half-life (mins)');
  $halfLife->setColors([$colors->[0],$colors->[1]]);
  $halfLife->setElementNameMarginSize(4);
  $halfLife->setPartName('half-life');
  $halfLife->setPlotTitle("Halflife - $id");
  $self->setGraphObjects($halfLife);

  return $self;

}

1;
