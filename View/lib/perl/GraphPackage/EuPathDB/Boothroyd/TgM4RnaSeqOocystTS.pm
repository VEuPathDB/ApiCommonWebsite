package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TgM4RnaSeqOocystTS;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinSenseRpkmProfileSet("T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome. - sense strand");
  $self->setMinAntisenseRpkmProfileSet("T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome. - antisense strand");

  $self->setDiffSenseRpkmProfileSet("T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome. - sense strand - diff");
  $self->setDiffAntisenseRpkmProfileSet("T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome. - antisense strand - diff");

  $self->setPctSenseProfileSet("percentile - T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome. - sense strand");
  $self->setPctAntisenseProfileSet("percentile - T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome. - antisense strand");

  $self->setColor("#995C7A");

  $self->makeGraphs(@_);

  return $self;
}

