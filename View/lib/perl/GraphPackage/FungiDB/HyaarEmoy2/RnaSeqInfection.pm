package ApiCommonWebsite::View::GraphPackage::FungiDB::HyaarEmoy2::RnaSeqInfection;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["Experiment1", "Experiment2", "Experiment3", "Experiment4", "Experiment5", "Experiment6"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet('H. arabidopsidis expression during infection');
  $self->setDiffRpkmProfileSet('H. arabidopsidis expression during infection profile');
  $self->setPctProfileSet('percentile - H. arabidopsidis expression during infection');
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(8);
  return $self;
}

1;
