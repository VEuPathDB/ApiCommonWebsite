package ApiCommonWebsite::View::GraphPackage::FungiDB::PhyraPr102::PhyraCondition;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["Chlyma.fastq", "Spores.fastq", "Tomato.fastq", "V8Liq.fastq"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("P ramorum Comprehensive Annotation");
  $self->setDiffRpkmProfileSet("P ramorum Comprehensive Annotation-diff");
  $self->setPctProfileSet("percentile - P ramorum Comprehensive Annotation");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(8);
  return $self;
}

1;
