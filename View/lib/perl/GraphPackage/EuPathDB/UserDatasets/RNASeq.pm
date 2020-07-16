package ApiCommonWebsite::View::GraphPackage::EuPathDB::UserDatasets::RNASeq;

use strict;
use vars qw( @ISA );

use Data::Dumper;

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;

use EbrcWebsiteCommon::View::GraphPackage::ProfileSet;

use LWP::Simple;
use JSON;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $id = $self->getId();
  my $datasetId = $self->getDatasetId();

  my $dbh = $self->getQueryHandle();
  my $url = $self->getBaseUrl() . '/a/service/profileSet/ProfileSetIds/' . $datasetId;
  my $content = get($url);
  my $json = from_json($content);

  my $sql = "select profile_set_id, name, unit from apidbuserdatasets.ud_profileset where user_dataset_id = $datasetId";

  my @profileSets;
  my %units;
  foreach my $profile (@$json) {
    my $psId = $profile->{'PROFILE_SET_ID'};
    my $psName = $profile->{'NAME'};
    my $psUnit = $profile->{'UNIT'};
    $units{$psUnit}=1;
    my $profileSet = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
    $profileSet->setJsonForService("{\"profileSetId\":\"$psId\",\"name\":\"$psName\"}");
    $profileSet->setSqlName("UserDatasets");

    push @profileSets, $profileSet;
  }

  die "Graph error: There is more than one unit type for dataset $datasetId\n" if (scalar keys %units > 1);
  my $yAxisUnit = (keys %units)[0];
  my $bar = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);

  $bar->setDefaultYMin(0);
  $bar->setProfileSets(\@profileSets);
  $bar->setYaxisLabel($yAxisUnit);
  $bar->setPartName($yAxisUnit);
  $bar->setPlotTitle("$id - UserDataset $datasetId");
  $bar->addAdjustProfile('
profile.df.full$NAME <- abbreviate(profile.df.full$NAME, 10)
profile.df.full$LEGEND <- profile.df.full$PROFILE_SET
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
    my $yAxisUnitLogged = "log2(".$yAxisUnit." + 1)";
    $bar->setYaxisLabel($yAxisUnitLogged);
    $bar->setIsLogged(1);
    $bar->setDefaultYMax(4);
    $bar->setSkipStdErr(1);
  }


  $self->setGraphObjects($bar);

  

  return $self;
}

1;
