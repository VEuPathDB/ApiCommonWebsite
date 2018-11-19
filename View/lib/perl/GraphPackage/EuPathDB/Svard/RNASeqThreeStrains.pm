package ApiCommonWebsite::View::GraphPackage::EuPathDB::Svard::RNASeqThreeStrains;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq );
use EbrcWebsiteCommon::View::GraphPackage::SimpleStrandSpecificRNASeq;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setMinSenseRpkmProfileSet("Strand-specific RNA-Seq from trophozoites of isolate WB and P15 and GS - sense strand");
  $self->setMinAntisenseRpkmProfileSet("Strand-specific RNA-Seq from trophozoites of isolate WB and P15 and GS - antisense strand");

  $self->setDiffSenseRpkmProfileSet("Strand-specific RNA-Seq from trophozoites of isolate WB and P15 and GS - sense strand - diff");
  $self->setDiffAntisenseRpkmProfileSet("Strand-specific RNA-Seq from trophozoites of isolate WB and P15 and GS - antisense strand - diff");

  $self->setPctSenseProfileSet("percentile - Strand-specific RNA-Seq from trophozoites of isolate WB and P15 and GS - sense strand");
  $self->setPctAntisenseProfileSet("percentile - Strand-specific RNA-Seq from trophozoites of isolate WB and P15 and GS - antisense strand");

  $self->setColor("#8F006B");
  $self->setIsPairedEnd(1);
  $self->setBottomMarginSize(4);

  $self->makeGraphs(@_);

  return $self;


}
