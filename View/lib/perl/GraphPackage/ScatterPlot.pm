package ApiCommonWebsite::View::GraphPackage::ScatterPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
use ApiCommonWebsite::View::GraphPackage::LinePlot;

sub new {
  my $class = shift;
  my $self = $class->SUPER::new(@_);

  $self->setPartName('xy_scatter');
  $self->setYaxisLabel("Expression Values");

  $self->setForceNoLines(1);
  $self->setVaryGlyphByXAxis(1);

   $self->setXaxisLabel("");
  return $self;
}

package ApiCommonWebsite::View::GraphPackage::ScatterPlot::LogRatio;
use base qw( ApiCommonWebsite::View::GraphPackage::ScatterPlot );
use strict;

sub new {
  my $class = shift;
  my $self = $class->SUPER::new(@_);
  my $id = $self->getId();

  $self->setPlotTitle("Expression Values - log(ratio) - $id");

  $self->setMakeYAxisFoldInduction(1);
  $self->setIsLogged(1);

   return $self;
}

1;



1;

