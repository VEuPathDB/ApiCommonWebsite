package ApiCommonWebsite::View::GraphPackage::FungiDB::HyaarEmoy2::HyaarInfection;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["Experiment1", "Experiment2", "Experiment3", "Experiment4", "Experiment5", "Experiment6"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("H arabidopsidis Comprehensive Annotation");
  $self->setDiffRpkmProfileSet("H arabidopsidis Comprehensive Annotation-diff");
  $self->setPctProfileSet("percentile - H arabidopsidis Comprehensive Annotation");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(8);
  return $self;
}

1;
