package ApiCommonWebsite::View::GraphPackage::Templates::SpliceSites;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

use ApiCommonWebsite::View::GraphPackage::Util;

use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub getPlotType {
  my $self = shift;

  return ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStackedSpliced->new(@_);
}

sub init {
  my $self = shift;
  $self->SUPER::init(@_);


  my $datasetName = $self->getDataset();


  my $dbh = $self->getQueryHandle();

  my $sql = ApiCommonWebsite::View::GraphPackage::Util::getProfileSetsSql();

  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetName);


  my ($minProfile, $diffProfile, $percentileProfile, $count);

  while(my ($profileName) = $sh->fetchrow_array()) {
    if($profileName =~ /\s?nonUniqProfile/) {
      $diffProfile = $profileName;
    } elsif($profileName =~ /percentile\s?-/) {
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

  return $self;
}


sub getSampleNames {
  my ($self) = @_;
  my $sampleLabelsString = $self->getSampleLabelsString();

  if ($sampleLabelsString) {
    my @rv = split(/;/, $sampleLabelsString);
    return \@rv;
  } else {
    return  $_[0]->{_sample_names};
  }
}

sub setForceXLabelsHorizontalString {$_[0]->{_force_x_labels_horizontal} = $_[1]}
sub getForceXLabelsHorizontalString {$_[0]->{_force_x_labels_horizontal}}

1;

#--------------------------------------------------------------------------------


# TEMPLATE_ANCHOR spliceSitesGraph

