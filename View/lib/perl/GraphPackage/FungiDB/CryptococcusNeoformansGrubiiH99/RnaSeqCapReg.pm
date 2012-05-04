package ApiCommonWebsite::View::GraphPackage::FungiDB::CryptococcusNeoformansGrubiiH99::RnaSeqCapReg;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["WT-30C", "WT-37C", "ada2-30C", "ada2-37C", "cir1-30C", "cir1-37C", "nrg1-30C", "nrg1-37C"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet('C.neoformans- ada2-delta, nrg1-delta, cir1-delta and KN99-alpha analysis');
  $self->setDiffRpkmProfileSet('C.neoformans- ada2-delta, nrg1-delta, cir1-delta and KN99-alpha analysis-diff');
  $self->setPctProfileSet('percentile - C.neoformans- ada2-delta, nrg1-delta, cir1-delta and KN99-alpha analysis');
  $self->setColor('#29ACF2');
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(6);

  return $self;
}


1;


 

