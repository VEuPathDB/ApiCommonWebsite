package ApiCommonWebsite::View::GraphPackage::EuPathDB::WGCNA::Eigengene;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;
use LWP::Simple;
use JSON;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  my $datasetId = $self->getDatasetId();
  my $url = $self->getBaseUrl() . '/a/service/profileSet/ProfileSetNames/' . $datasetId;
  my $content = get($url);
  my $json = from_json($content);

  my $profileSetsHash;
  foreach my $profile (@$json) {
    my $profileName = $profile->{'PROFILE_SET_NAME'};
    my $profileType = $profile->{'PROFILE_TYPE'};
    next if($self->isExcludedProfileSet($profileName));
    $profileSetsHash = {profileSetName=>$profileName, profileType=>$profileType};
  }
  my $jsonForService = encode_json($profileSetsHash);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("eigengene_profileset");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");

  my $barplot = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::WGCNA->new(@_);
  $barplot->setProfileSets([$profileSets]);


  $self->setGraphObjects($barplot);

  return $self;
}


sub isExcludedProfileSet {
  my ($self, $name) = @_;

  if($name =~ /eigengene/) {
    return 0;
  }
  return 1;
}


1;
