package ApiCommonWebsite::View::GraphPackage::FungiDB::HyaarEmoy2::RnaSeqInfection;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["Replicate1", "Replicate2", "Replicate3", "Replicate4", "Replicate5", "Replicate6"];

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
