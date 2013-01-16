package ApiCommonWebsite::View::GraphPackage::MicrosporidiaDB::Troemel::CelegansInfectionTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeqLinePlot );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeqLinePlot;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet("C. elegans Time Series - Infected");
  $self->setDiffRpkmProfileSet("C. elegans Time Series - Infected-diff");
  $self->setPctProfileSet("percentile - C. elegans Time Series - Infected");
  $self->setColor("#D87093");

  $self->setXAxisLabel("hours");
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(6);


  return $self;
}

1;
