package ApiCommonWebsite::View::GraphPackage::FungiDB::RhizopusOryzae::RnaSeqHyphalTip;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames = ['RO3H','RO5H','RO20H'];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet('rhizopus_oryzae_99880 hyphal_tip RNA Sequence min Profiles');
  $self->setDiffRpkmProfileSet('rhizopus_oryzae_99880 hyphal_tip RNA Sequence diff Profiles');
  $self->setPctProfileSet('rhizopus_oryzae_99880 hyphal_tip RNA Sequence min Profiles Percentile');
  $self->setColor('#D87093');
  $self->makeGraphs(@_);


  return $self;
}


1;
