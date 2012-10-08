package ApiCommonWebsite::View::GraphPackage::ToxoDB::Reid::TgME49RnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $sampleNames =["Day3", "Day4"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet('T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome');
  $self->setDiffRpkmProfileSet('T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome -diff');
  $self->setPctProfileSet('percentile - T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome');
  $self->setColor("#6A5ACD");
  $self->makeGraphs(@_);

  return $self;
}

1;



