package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TgM4RnaSeqOocystTS;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet('T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome.');
  $self->setDiffRpkmProfileSet('T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome.-diff');
  $self->setPctProfileSet('percentile - T. gondii M4 oocyte time series mRNA Illumina sequences aligned to the ME49 Genome.');
  $self->setColor("orange");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(4);

  return $self;
}

1;



