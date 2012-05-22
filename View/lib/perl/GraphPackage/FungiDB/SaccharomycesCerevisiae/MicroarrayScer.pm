package ApiCommonWebsite::View::GraphPackage::FungiDB::SaccharomycesCerevisiae::MicroarrayScer

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#B22222', 'darkblue' ];

my @profileSetNames = (['']);
