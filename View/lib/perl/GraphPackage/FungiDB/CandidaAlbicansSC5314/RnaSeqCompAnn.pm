package ApiCommonWebsite::View::GraphPackage::FungiDB::CandidaAlbicansSC5314::RnaSeqCompAnn;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet("C albicans comprehensive annotation");
  $self->setDiffRpkmProfileSet("C albicans comprehensive annotation - diff");
  $self->setPctProfileSet("percentile - C albicans comprehensive annotation");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(4);
  return $self;
}

1;
