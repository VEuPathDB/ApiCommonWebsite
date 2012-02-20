package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::Combined;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;


use ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::halflife;
use ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::3D7;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['purple', 'darkred', 'green', 'orange']; # as in the paper!
  my $pch = [19,24,20,23];
  my $legend = ['Ring', 'Trophozoite', 'Schizont', 'Late schiz.'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch});


  my $hl = ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::halflife->new();
  my $lines = ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiHalfLife::3D7->new();

  $self->setGraphObjects($hl, $lines);

  return $self;
}


1;

