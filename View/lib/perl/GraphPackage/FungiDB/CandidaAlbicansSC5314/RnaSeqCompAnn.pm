package ApiCommonWebsite::View::GraphPackage::FungiDB::CandidaAlbicansSC5314::RnaSeqCompAnn;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["Cellwall Damaging Environment", "High Oxidative Stress", "Low Oxidative Stress", "Media Containing Serum", "medium at PH4", "medium at PH8", "Nitrosative Stress", "Non Cellwall Damage Environment", "Non Nitrosative Environment", "Non Oxidation Stress", "YPD", "YPD media", "YPD media containing Serum"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("C albicans Comprehensive Annotation");
  $self->setDiffRpkmProfileSet("C albicans Comprehensive Annotation-diff");
  $self->setPctProfileSet("percentile - C albicans Comprehensive Annotation");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(4);
  return $self;
}

1;
