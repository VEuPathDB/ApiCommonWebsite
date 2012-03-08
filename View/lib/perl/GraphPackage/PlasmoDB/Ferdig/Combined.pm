package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Combined;

use strict;
use vars qw( @ISA );

use Data::Dumper;


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::DD2_X_HB3;
use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Pcts;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(550);

  my $expr = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::DD2_X_HB3->new({QueryHandle => $self->getQueryHandle()});
  my $pct = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Pcts->new({QueryHandle => $self->getQueryHandle()});

  $self->setMainLegend($expr->getMainLegend());

  $self->setGraphObjects($expr, $pct);

  return $self;
}

1;

