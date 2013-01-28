package ApiCommonWebsite::View::GraphPackage::FungiDB::CimmRS::CimmComTranRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames = ["Hyphae","Spherules"];

  #$self->setPlotWidth(800);
  $self->setBottomMarginSize(6);

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("C immitis saprobic hyphae and parasitic spherules");
  $self->setDiffRpkmProfileSet("C immitis saprobic hyphae and parasitic spherules-diff");
  $self->setPctProfileSet("percentile - C immitis saprobic hyphae and parasitic spherules");
  $self->setColor("#29ACF2");
  $self->makeGraphs(@_);

  return $self;
}


1;
