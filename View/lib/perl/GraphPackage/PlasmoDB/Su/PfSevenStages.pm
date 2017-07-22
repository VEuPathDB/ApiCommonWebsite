package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Su::PfSevenStages;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet("P. falciparum Su Seven Stages RNA Seq data");
  $self->setDiffRpkmProfileSet("P. falciparum Su Seven Stages RNA Seq data-diff");
  $self->setPctProfileSet("percentile - P. falciparum Su Seven Stages RNA Seq data");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(8);

  return $self;
}

1;
