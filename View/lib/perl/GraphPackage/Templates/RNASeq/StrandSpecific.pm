package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::StrandSpecific;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleStrandSpecificRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleStrandSpecificRNASeq;


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $datasetName = $self->getDataset();

  my $dbh = $self->getQueryHandle();

  my $sql = "select ps.name
from apidb.profileset ps, sres.externaldatabase d, sres.externaldatabaserelease r
where ps.external_database_release_id = r.external_database_release_id
and r.external_database_id = d.external_database_id
and d.name = ?";

  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetName);


  my ($minSenseProfile, $minAntisenseProfile, $diffSenseProfile, $diffAntisenseProfile, $percentileSenseProfile, $percentileAntisenseProfile, $count);

  while(my ($profileName) = $sh->fetchrow_array()) {
    if($profileName =~ /-\s?sense strand\s?-\s?diff$/) {
      $diffSenseProfile = $profileName;
    } elsif($profileName =~ /-\s?antisense strand\s?-\s?diff$/) {
      $diffAntisenseProfile = $profileName;
    } elsif($profileName =~ /^percentile\s?-.*-\s?sense strand$/) {
      $percentileSenseProfile = $profileName;
    } elsif($profileName =~ /^percentile\s?-.*-\s?antisense strand$/) {
      $percentileAntisenseProfile = $profileName;
    } elsif($profileName =~ /antisense strand$/) {
      $minSenseProfile = $profileName;
    } elsif($profileName =~ /sense strand$/) {
      $minAntisenseProfile = $profileName;
    } else {
      next; # so we do not add to count
    }
    $count++;
  }
  $sh->finish();

  die "Expected 6 profile sets but got $count for $datasetName!!" if($count != 6);


  $self->setMinSenseRpkmProfileSet($minSenseProfile);
  $self->setMinAntisenseRpkmProfileSet($minAntisenseProfile);

  $self->setDiffSenseRpkmProfileSet($diffSenseProfile);
  $self->setDiffAntisenseRpkmProfileSet($diffAntisenseProfile);

  $self->setPctSenseProfileSet($percentileSenseProfile);
  $self->setPctAntisenseProfileSet($percentileAntisenseProfile);

  return $self;
}

1;

#--------------------------------------------------------------------------------


# TEMPLATE_ANCHOR rnaSeqStrandSpecificGraph

