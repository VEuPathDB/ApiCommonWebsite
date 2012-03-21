package ApiCommonWebsite::View::GraphPackage::RNASeqStackedBarPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::Util;

#--------------------------------------------------------------------------------

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);
   $self->setPartName('coverage');
   $self->setYaxisLabel('Normalized Coverage (log2)');
   $self->setIsStacked(1);
   $self->setIsLogged(1);
   $self->setDefaultYMin(4);
   $self->setAdjustProfile('profile=profile + 1; profile = log2(profile);');
   
   return $self;
}
