package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Su::PfStrandSpecific;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleStrandSpecificRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleStrandSpecificRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

#================================================================================
#   NOTE!!!!
#    THESE ARE PATCHED FOR BUILD16 BECAUSE SENSE/ANTISENSE WERE SWAPPED IN LOADING
#================================================================================

  $self->setMinAntisenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - sense strand");
  $self->setMinSenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - antisense strand");

  $self->setDiffAntisenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - sense strand - diff");
  $self->setDiffSenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - antisense strand - diff");

  $self->setPctAntisenseProfileSet("percentile - P. falciparum Su Strand Specific RNA Seq data - sense strand");
  $self->setPctSenseProfileSet("percentile - P. falciparum Su Strand Specific RNA Seq data - antisense strand");

  $self->setColor("#8F006B");

  $self->makeGraphs(@_);

  return $self;


}
