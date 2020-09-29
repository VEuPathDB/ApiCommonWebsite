package ApiCommonWebsite::View::GraphPackage::EuPathDB::Barrett::purineStarvation;

use strict;
use vars qw( @ISA);

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;
use Data::Dumper;
use LWP::Simple;
use JSON;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setFacets(["pH"]);
  $self->setPlotWidth(600);

  my $compoundId = $self->getId();

  my $colors = ['dodgerblue', 'slateblue'];

  my $elementNames = ['Log Growth|6.4', 'Purine Starved Growth|6.4'];

  my $url = $self->getBaseUrl() . '/a/service/profileSet/CompoundPeaksIdentifier/' . $compoundId;

  my $content = get($url);
  my $json = from_json($content);


  my $jsonForService;

  foreach my $result (@$json) {
  
    my $compoundpeaks = $result->{'COMPOUNDPEAKSIDENTIFIER'};


      if ($compoundpeaks) {

      if (defined $jsonForService) {
        $jsonForService = $jsonForService . ",{\"profileSetName\":\"Barrett_PurineStarvation [metaboliteProfiles]\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$compoundpeaks\",\"name\":\"$compoundId|$compoundpeaks\"}";
      } else {
        $jsonForService = "{\"profileSetName\":\"Barrett_PurineStarvation [metaboliteProfiles]\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$compoundpeaks\",\"name\":\"$compoundId|$compoundpeaks\"}";
      }
    }
  }


  if (!defined $jsonForService) {
    $jsonForService = "{\"profileSetName\":\"Barrett_PurineStarvation [metaboliteProfiles]\",\"profileType\":\"values\",\"name\":\"$compoundId\"}";
  }
    
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");


  my $massSpec = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::MassSpec->new(@_);
  my $rAdjustString = <<'RADJUST';
    profile.df.full <- profile.df.full[!grepl("blank", profile.df.full$NAME),]
    profile.df.full$LEGEND=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,1];
    profile.df.full$pH=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,2];

    library(stringi);
    
    profile.df.full$STACK=profile.df.full$STACK=matrix(unlist(stri_split_fixed(str = as.character(profile.df.full$DISPLAY_NAME), pattern = "|", n = 2)), ncol=2, byrow=T)[,2];

    times=length(unique(profile.df.full$STACK));
    profile.df.full$order = matrix(unlist(rep(1:2, times=times)));
    profile.df.full$NAME = factor(profile.df.full$LEGEND);
    profile.df.full$LEGEND = factor(profile.df.full$LEGEND);
    #profile.df.full <- profile.df.full[order(as.numeric(profile.df.full$order)),];                                                 
    profile.df.full$STACK = factor(profile.df.full$STACK, levels=rev(unique(profile.df.full$STACK)));
    hideLegend <- FALSE;
RADJUST
  $massSpec->setAdjustProfile($rAdjustString);
  $massSpec->setProfileSets([$profileSets]);
  $massSpec->setColors($colors);
  $massSpec->setDefaultYMax(100);
  $massSpec->setSampleLabels($elementNames);

  $massSpec->setIsStacked(1);
  $massSpec->setHideXAxisLabels(1);
  $massSpec->setLegendColors($colors);
  $massSpec->setLegendLabels(['Log Growth', 'Purine Starved Growth']);

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
