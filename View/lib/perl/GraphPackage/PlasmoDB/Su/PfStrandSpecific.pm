package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Su::PfStrandSpecific;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data");
  $self->setDiffRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data-diff");
  $self->setPctProfileSet("percentile - P. falciparum Su Strand Specific RNA Seq data");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(8);

  return $self;
}

1;
