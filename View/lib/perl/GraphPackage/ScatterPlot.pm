package ApiCommonWebsite::View::GraphPackage::ScatterPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlotSet );
use ApiCommonWebsite::View::GraphPackage::LinePlotSet;


sub init {
  my $self = shift;
  my $args = ref $_[0] ? shift : {@_};

  $self->SUPER::init($args);

  # Defaults
  $self->setForceNoLines(1);
  $self->setVaryGlyphByXAxis(1);

  return $self;
}


1
