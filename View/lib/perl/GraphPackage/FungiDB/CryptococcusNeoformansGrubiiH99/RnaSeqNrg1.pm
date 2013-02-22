package ApiCommonWebsite::View::GraphPackage::FungiDB::CryptococcusNeoformansGrubiiH99::RnaSeqNrg1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(5.5);

  my $sampleNames = ['Wild Type', 'nrg1 KO', 'nrg1 overexp'];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet('C.neoformans NRG1 Expression');
  $self->setDiffRpkmProfileSet('C.neoformans NRG1 Expression-diff');
  $self->setPctProfileSet('percentile - C.neoformans NRG1 Expression');
  $self->setColor("#D87093");
  $self->setIsPairedEnd(1);
  $self->makeGraphs(@_);


  return $self;
}


1;

