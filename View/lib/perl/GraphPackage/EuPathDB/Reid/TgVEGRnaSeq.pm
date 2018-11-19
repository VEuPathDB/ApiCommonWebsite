package ApiCommonWebsite::View::GraphPackage::EuPathDB::Reid::TgVEGRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet("T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome");
  $self->setDiffRpkmProfileSet("T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome - diff");
  $self->setPctProfileSet("percentile - T. gondii VEG Day 3-4 Tachyzoite aligned to the TgME49 Genome");
  $self->setColor("#E6CC80");
  $self->setIsPairedEnd(1);
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(4.5);

  return $self;
}

1;


