package ApiCommonWebsite::View::GraphPackage::EuPathDB::Gregory::TgME49RnaSeqHuman;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinSenseRpkmProfileSet("T. gondii ME49 infection - time series mRNA Illumina sequences aligned to the human genome. - sense strand");
  $self->setMinAntisenseRpkmProfileSet("T. gondii ME49 infection - time series mRNA Illumina sequences aligned to the human genome. - antisense strand");

  $self->setDiffSenseRpkmProfileSet("T. gondii ME49 infection - time series mRNA Illumina sequences aligned to the human genome. - sense strand - diff");
  $self->setDiffAntisenseRpkmProfileSet("T. gondii ME49 infection - time series mRNA Illumina sequences aligned to the human genome. - antisense strand - diff");

  $self->setPctSenseProfileSet("percentile - T. gondii ME49 infection - time series mRNA Illumina sequences aligned to the human genome. - sense strand");
  $self->setPctAntisenseProfileSet("percentile - T. gondii ME49 infection - time series mRNA Illumina sequences aligned to the human genome. - antisense strand");

  $self->setColor("#6600CC");

  $self->makeGraphs(@_);

  return $self;


}
