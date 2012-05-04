package ApiCommonWebsite::View::GraphPackage::FungiDB::NeurosporaCrassaOR74A::RnaSeqPopGen;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(800);
  $self->setBottomMarginSize(7);


  $self->setAdditionalRCode("colnames(profile.df) =  sub(\".fastq\", \"\", colnames(profile.df));");
  $self->setMinRpkmProfileSet("N Crassa population genomics");
  $self->setDiffRpkmProfileSet("N Crassa population genomics-diff");
  $self->setPctProfileSet("percentile - N Crassa population genomics");
  $self->setColor("#29ACF2");
  $self->makeGraphs(@_);


  return $self;
}


1;
