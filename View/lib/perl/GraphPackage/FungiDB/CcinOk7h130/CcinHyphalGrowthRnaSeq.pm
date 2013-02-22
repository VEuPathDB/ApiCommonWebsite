package ApiCommonWebsite::View::GraphPackage::FungiDB::CcinOk7h130::CcinHyphalGrowthRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames = ["Cc-9D", "Cc-15D"];

  #$self->setPlotWidth(800);
  $self->setBottomMarginSize(5);

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("C cinereus hyphal growth expression");
  $self->setDiffRpkmProfileSet("C cinereus hyphal growth expression-diff");
  $self->setPctProfileSet("percentile - C cinereus hyphal growth expression");
  $self->setColor("#29ACF2");
  $self->setIsPairedEnd(1);
  $self->makeGraphs(@_);

  return $self;
}


1;
