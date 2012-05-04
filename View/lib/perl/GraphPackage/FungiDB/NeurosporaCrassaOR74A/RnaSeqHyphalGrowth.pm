package ApiCommonWebsite::View::GraphPackage::FungiDB::NeurosporaCrassaOR74A::RnaSeqHyphalGrowth;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet("NcraOR74A Hyphal Growth RNASeq");
  $self->setDiffRpkmProfileSet("NcraOR74A Hyphal Growth RNASeq - diff");
  $self->setPctProfileSet("percentile - NcraOR74A Hyphal Growth RNASeq");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(4);
  return $self;
}

1;
