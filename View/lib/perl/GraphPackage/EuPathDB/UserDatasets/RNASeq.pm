package ApiCommonWebsite::View::GraphPackage::EuPathDB::UserDatasets::RNASeq;

use strict;
use vars qw( @ISA );

use Data::Dumper;

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;

use EbrcWebsiteCommon::View::GraphPackage::ProfileSet;

use EbrcWebsiteCommon::Model::CannedQuery::UDProfileValues;
use EbrcWebsiteCommon::Model::CannedQuery::UDProfileNames;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $id = $self->getId();
  my $datasetId = $self->getDatasetId();

  my $dbh = $self->getQueryHandle();
  my $sql = "select profile_set_id, name from apidbuserdatasets.ud_profileset where user_dataset_id = $datasetId";
  my $sh = $dbh->prepare($sql);
  $sh->execute();

  my @profileSets;
  while(my ($psId, $psName) = $sh->fetchrow_array()) {
    my $udValuesQuery = EbrcWebsiteCommon::Model::CannedQuery::UDProfileValues->new(Id => $id, ProfileSetId => $psId);
    my $udNamesQuery = EbrcWebsiteCommon::Model::CannedQuery::UDProfileNames->new(ProfileSetId => $psId);

    my $profileSet = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
    $profileSet->setProfileCannedQuery($udValuesQuery);
    $profileSet->setProfileNamesCannedQuery($udNamesQuery);

    push @profileSets, $profileSet;
  }
  $sh->finish();

  my $bar = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);

  $bar->setDefaultYMin(0);
  $bar->setProfileSets(\@profileSets);
  $bar->setYaxisLabel("fpkm");
  $bar->setColors(["Violet"]);
  $bar->setPartName("fpkm");
  $bar->setPlotTitle("$id - UserDataset $datasetId");

  $self->setGraphObjects($bar);

  

  return $self;
}

1;
