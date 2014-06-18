package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::StrandSpecific;

use strict;
use vars qw( @ISA );

# TODO: Update so we can create multiple rnaseq profile sets (See StransNonSpecific module)

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleStrandSpecificRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleStrandSpecificRNASeq;

use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $datasetName = $self->getDataset();

  my $id = $self->getId();

  my $dbh = $self->getQueryHandle();

  my $sql = ApiCommonWebsite::View::GraphPackage::Util::getProfileSetsSql();

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
      $minAntisenseProfile = $profileName;
    } elsif($profileName =~ /sense strand$/) {
      $minSenseProfile = $profileName;
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

