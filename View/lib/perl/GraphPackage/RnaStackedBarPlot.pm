package ApiCommonWebsite::View::GraphPackage::PercentilePlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::Util;

use Data::Dumper;

#--------------------------------------------------------------------------------

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);
   $self->setPartName('coverage');
   $self->setYaxisLabel('Normalized Coverage (log2)');
   $self->setIsStacked(1);
   $self->setIsLogged(1);

   
   return $self;
}
