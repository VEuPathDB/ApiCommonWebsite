package ApiCommonWebsite::View::GraphPackage::CryptoDB::Lippuner::CpSimpleRNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $sampleNames = ["CL1", "CL2", "CL3", "CL4"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("CparIowaII Lippuner mRNA Seq data");
  $self->setDiffRpkmProfileSet("CparIowaII Lippuner mRNA Seq data-diff");
  $self->setPctProfileSet("percentile - CparIowaII Lippuner mRNA Seq data");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(8);

  return $self;
}

1;
