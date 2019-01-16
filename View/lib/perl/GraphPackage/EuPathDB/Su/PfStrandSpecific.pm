package ApiCommonWebsite::View::GraphPackage::EuPathDB::Su::PfStrandSpecific;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq;
#use EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinSenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - sense strand");
  $self->setMinAntisenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - antisense strand");

  $self->setDiffSenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - sense strand - diff");
  $self->setDiffAntisenseRpkmProfileSet("P. falciparum Su Strand Specific RNA Seq data - antisense strand - diff");

  $self->setPctSenseProfileSet("percentile - P. falciparum Su Strand Specific RNA Seq data - sense strand");
  $self->setPctAntisenseProfileSet("percentile - P. falciparum Su Strand Specific RNA Seq data - antisense strand");

  $self->setIsPairedEnd(1);
  $self->setColor("#8F006B");

  $self->makeGraphs(@_);

  return $self;


}
