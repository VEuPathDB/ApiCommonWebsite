package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Combined;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlots );
use ApiCommonWebsite::View::GraphPackage::MixedPlots;


use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::DD2_X_HB3;
use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Pcts;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  my $expr = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::DD2_X_HB3->new();
  my $pct = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Pcts->new();

  $self->setGraphObjects($expr, $pct);

  return $self;
}


1;

