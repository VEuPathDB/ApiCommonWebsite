package ApiCommonWebsite::View::GraphPackage::FungiDB::Spombe972h::RnaSeqDicer;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["dcr1Delta", "dcr1Deltaloop2", "dcr1-SHSS", "Wild Type"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet('S Pombe Dicer Protein');
  $self->setDiffRpkmProfileSet('S Pombe Dicer Protein-diff');
  $self->setPctProfileSet('percentile - S Pombe Dicer Protein');
  $self->setColor('#29ACF2');
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(6);

  return $self;
}


1;
