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
    my $udValuesQuery = EbrcWebsiteCommon::Model::CannedQuery::UDProfileValues->new(Id => $id, ProfileSetId => $psId, Name => $psName);
    my $udNamesQuery = EbrcWebsiteCommon::Model::CannedQuery::UDProfileNames->new(ProfileSetId => $psId, Name => $psName);

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
  $bar->setPartName("fpkm");
  $bar->setPlotTitle("$id - UserDataset $datasetId");
  $bar->addAdjustProfile('
profile.df.full$NAME <- abbreviate(profile.df.full$NAME, 10)
profile.df.full$LEGEND <- sapply(strsplit(profile.df.full$PROFILE_FILE, "-", fixed = TRUE), "[[", 3)
profile.df.full$LEGEND <- gsub(".tab", "", profile.df.full$LEGEND)
hideLegend = FALSE
');
  $bar->setRPostscript('
numColors = length(unique(profile.df.full$LEGEND))
gp = gp + scale_fill_manual(values=viridis(numColors, begin=.2, end=.8), breaks=profile.df.full$LEGEND, name="Legend")
gp = gp + scale_colour_manual(values=viridis(numColors, begin=.2, end=.8), breaks=profile.df.full$LEGEND, name="Legend")
');

  my $wantLogged = $bar->getWantLogged();
  if($wantLogged) {
    $bar->addAdjustProfile('profile.df.full$VALUE = log2(profile.df.full$VALUE + 1);');
    $bar->setYaxisLabel('FPKM (log2)');
    $bar->setIsLogged(1);
    $bar->setDefaultYMax(4);
    $bar->setSkipStdErr(1);
  }


  $self->setGraphObjects($bar);

  

  return $self;
}

1;
