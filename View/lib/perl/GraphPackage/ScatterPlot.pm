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

package ApiCommonWebsite::View::GraphPackage::ScatterPlot::ClinicalMetaData;
use base qw( ApiCommonWebsite::View::GraphPackage::ScatterPlot );
use strict;

sub new {
  my $class = shift;
  my $self = $class->SUPER::new(@_);
  my $id = $self->getId();
  my $metaDataCategory = $self->getTypeArg;

  $self->setPlotTitle("Signal Intensity Values - arcsinh(1+50*value) - $id");

  $self->setMakeYAxisFoldInduction(0);
  $self->setIsLogged(0);
  $self->setYaxisLabel('Signal Intensity');
  $self->setHasExtraLegend(1);
  $self->setExtraLegendSize(4.5);
  $self->setHasMetaData(1);
  $self->setDefaultYMax(1);
  $self->setDefaultYMin(-1);
  $self->setElementNameMarginSize(4);
  $self->setXaxisLabel("Samples colored based on $metaDataCategory");
   return $self;
}

1;



1;

