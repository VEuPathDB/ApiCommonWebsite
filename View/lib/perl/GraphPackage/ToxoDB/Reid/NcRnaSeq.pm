package ApiCommonWebsite::View::GraphPackage::ToxoDB::Reid::NcRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setMinRpkmProfileSet("N. caninum Day 3-4 Tachyzoite");
  $self->setDiffRpkmProfileSet("N. caninum Day 3-4 Tachyzoite - diff");
  $self->setPctProfileSet("percentile - N. caninum Day 3-4 Tachyzoite");
  $self->setColor("#6A5ACD");
  $self->setIsPairedEnd(1);
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(4.5);

  return $self;
}

1;


