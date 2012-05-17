package ApiCommonWebsite::View::GraphPackage::FungiDB::PhysoP6497::PhysoInfection;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["mycelial.fastq", "infected.fastq"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("P sojae Comprehensive Annotation");
  $self->setDiffRpkmProfileSet("P sojae Comprehensive Annotation-diff");
  $self->setPctProfileSet("percentile - P sojae Comprehensive Annotation");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(8);
  return $self;
}

1;
