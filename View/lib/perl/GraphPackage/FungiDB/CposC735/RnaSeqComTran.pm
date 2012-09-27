package ApiCommonWebsite::View::GraphPackage::FungiDB::CposC735::RnaSeqComTran;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  $self->setBottomMarginSize(6);
  my $sampleNames = ["Spherules", "Hyphae"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("C. posadasii saprobic hyphae and parasitic spherules");
  $self->setDiffRpkmProfileSet("C. posadasii saprobic hyphae and parasitic spherules-diff");
  $self->setPctProfileSet("percentile - C. posadasii saprobic hyphae and parasitic spherules");
  $self->setColor("#29ACF2");
  $self->makeGraphs(@_);


  return $self;
}


1;
