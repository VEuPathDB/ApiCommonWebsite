package ApiCommonWebsite::View::GraphPackage::FungiDB::PhyraPr102::RnaSeqCondition;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["Chlyma", "Spores", "Tomato", "V8Liq"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet('P. ramorum expression in Chlamydospora, P. ramorum expression in zoospores, P. ramorum expression is Tomato media, P. ramorum expression in V8 Liquid media');
  $self->setDiffRpkmProfileSet('P. ramorum expression in Chlamydospora, P. ramorum expression in zoospores, P. ramorum expression is Tomato media, P. ramorum expression in V8 Liquid media');
  $self->setPctProfileSet('percentile - P. ramorum expression in Chlamydospora, P. ramorum expression in zoospores, P. ramorum expression is Tomato media, P. ramorum expression in V8 Liquid media');
  $self->setColor('#29ACF2');
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(6);

  return $self;
}


1;
