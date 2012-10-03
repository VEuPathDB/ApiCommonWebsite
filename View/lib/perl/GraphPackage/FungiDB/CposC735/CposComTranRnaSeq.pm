package ApiCommonWebsite::View::GraphPackage::FungiDB::CposC735::CposComTranRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames = ["Spherules", "Hyphae"];

  #$self->setPlotWidth(800);
  $self->setBottomMarginSize(6);

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("C. posadasii saprobic hyphae and parasitic spherules");
  $self->setDiffRpkmProfileSet("C. posadasii saprobic hyphae and parasitic spherules-diff");
  $self->setPctProfileSet("percentile - C. posadasii saprobic hyphae and parasitic spherules");
  $self->setColor("#29ACF2");
  $self->makeGraphs(@_);

  return $self;
}


1;
