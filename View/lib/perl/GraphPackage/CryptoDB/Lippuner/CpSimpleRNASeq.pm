package ApiCommonWebsite::View::GraphPackage::CryptoDB::Lippuner::CpSimpleRNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  #my $sampleNames = ["CL1", "CL2", "CL3", "CL4"];
  my $sampleNames = ["1.2e9", "2.5e9"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("CparIowaII Lippuner calf intestine oocyst infection");
  $self->setDiffRpkmProfileSet("CparIowaII Lippuner calf intestine oocyst infection-diff");
  $self->setPctProfileSet("percentile - CparIowaII Lippuner calf intestine oocyst infection");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(5);

  return $self;
}

1;
