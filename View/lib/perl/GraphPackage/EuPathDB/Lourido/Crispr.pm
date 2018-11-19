package ApiCommonWebsite::View::GraphPackage::ToxoDB::Lourido::Crispr;

use strict;
use vars qw( @ISA );

use Data::Dumper;

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage );
use EbrcWebsiteCommon::View::GraphPackage;


sub run {
  my $self = shift;

  if($self->getCompact()) {
    $self->setOutputFile("$ENV{GUS_HOME}/../webapp/images/CrisprPhenotype_compact.png");
  }
  else {
    $self->setOutputFile("$ENV{GUS_HOME}/../webapp/images/CrisprPhenotype.svg");
  }
}

sub declareParts {
  return "phenotype_score";
}

1;


