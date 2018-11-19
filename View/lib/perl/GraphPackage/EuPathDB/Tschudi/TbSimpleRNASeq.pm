package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Tschudi::TbSimpleRNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $sampleNames = ["3-end oligo(dT)", "3-end random", "5-SL-end", "5-triphosphate-end"];

  $self->setSampleNames($sampleNames);
  $self->setMinRpkmProfileSet("T.brucei Tschudi RNA Seq data");
  $self->setDiffRpkmProfileSet("T.brucei Tschudi RNA Seq data-diff");
  $self->setPctProfileSet("percentile - T.brucei Tschudi RNA Seq data");
  $self->setColor("#D87093");
  $self->makeGraphs(@_);
  $self->setBottomMarginSize(8);

  return $self;
}

1;
