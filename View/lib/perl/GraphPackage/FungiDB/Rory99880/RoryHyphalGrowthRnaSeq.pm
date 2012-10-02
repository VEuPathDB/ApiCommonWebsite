package ApiCommonWebsite::View::GraphPackage::FungiDB::Rory99880::RoryHyphalGrowthRnaSeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  #$self->setPlotWidth(800);
  #$self->setBottomMarginSize(7);
  my $sampleNames = ['RO3H','RO5H','RO20H'];
  $self->setSampleNames($sampleNames);

  $self->setAdditionalRCode("colnames(profile.df) =  sub(\".fastq\", \"\", colnames(profile.df));");
  $self->setMinRpkmProfileSet("R oryzae hyphal growth on solid media");
  $self->setDiffRpkmProfileSet("R oryzae hyphal growth on solid media-diff");
  $self->setPctProfileSet("percentile - R oryzae hyphal growth on solid media");
  $self->setColor("#29ACF2");
  $self->makeGraphs(@_);

  return $self;
}


1;
