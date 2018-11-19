package ApiCommonWebsite::View::GraphPackage::EuPathDB::Sibley::TgME49Bradyzoite;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinSenseRpkmProfileSet("T. gondii ME49 bradyzoite mRNA Illumina sequences aligned to the ME49 Genome. - sense strand");
  $self->setMinAntisenseRpkmProfileSet("T. gondii ME49 bradyzoite mRNA Illumina sequences aligned to the ME49 Genome. - antisense strand");

  $self->setDiffSenseRpkmProfileSet("T. gondii ME49 bradyzoite mRNA Illumina sequences aligned to the ME49 Genome. - sense strand - diff");
  $self->setDiffAntisenseRpkmProfileSet("T. gondii ME49 bradyzoite mRNA Illumina sequences aligned to the ME49 Genome. - antisense strand - diff");

  $self->setPctSenseProfileSet("percentile - T. gondii ME49 bradyzoite mRNA Illumina sequences aligned to the ME49 Genome. - sense strand");
  $self->setPctAntisenseProfileSet("percentile - T. gondii ME49 bradyzoite mRNA Illumina sequences aligned to the ME49 Genome. - antisense strand");

  $self->setColor("#B40404");

  $self->makeGraphs(@_);

  return $self;
}

