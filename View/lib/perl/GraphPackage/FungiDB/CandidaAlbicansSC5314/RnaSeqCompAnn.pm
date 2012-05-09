package ApiCommonWebsite::View::GraphPackage::FungiDB::CandidaAlbicansSC5314::RnaSeqCompAnn;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["CelwalDamageEnv", "HighOxi_Stress", "LowOxi_Stress", "Media_Serum", "medium_PH4", "medium_PH8", "Nitro_Stress", "N_CelwalDamageEnv", "N_NitroEnv", "N_Oxi_Stress", "YPD", "YPDmedia", "YPDmedia_Serum"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("C albicans Comprehensive Annotation");
  $self->setDiffRpkmProfileSet("C albicans Comprehensive Annotation-diff");
  $self->setPctProfileSet("percentile - C albicans Comprehensive Annotation");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);

  $self->setBottomMarginSize(8);
  return $self;
}

1;
