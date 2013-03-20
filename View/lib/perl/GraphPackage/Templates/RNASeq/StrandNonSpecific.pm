package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::StrandNonSpecific;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);


  my $datasetName = $self->getSecondaryId();

  my $dbh = $self->getQueryHandle();

  my $sql = "select ps.name
from apidb.profileset ps, sres.externaldatabase d, sres.externaldatabaserelease r
where ps.external_database_release_id = r.external_database_release_id
and r.external_database_id = d.external_database_id
and d.name = ?";

  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetName);


  my ($minProfile, $diffProfile, $percentileProfile, $count);

  while(my ($profileName) = $sh->fetchrow_array()) {
    if($profileName =~ /-\s?diff$/) {
      $diffProfile = $profileName;
    } elsif($profileName =~ /^percentile\s?-/) {
      $percentileProfile = $profileName;
    } else {
      $minProfile = $profileName;
    }
    $count++;
  }
  $sh->finish();

  die "Expected 3 rows in profileset for $datasetName" if($count != 3);


  $self->setMinRpkmProfileSet($minProfile);
  $self->setDiffRpkmProfileSet($diffProfile);
  $self->setPctProfileSet($percentileProfile);


  # TODO:  Need to inject these from props
  $self->setIsPairedEnd(1);
  $self->setColor("#D87093");
  $self->setBottomMarginSize(8);

  $self->makeGraphs(@_);


  return $self;
}

1;
