package ApiCommonWebsite::View::GraphPackage::EuPathDB::Llinas::pHMetabolite;

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

  my $colors = ['dodgerblue', 'slateblue', 'forestgreen', '#2F4F4F', 'salmon', '#E9967A'];

  my $elementNames = ['blank pellet', 'Infected RBC pellet|6.4', 'Infected RBC pellet|7.4', 'Infected RBC pellet|8.4', 'Uninfected RBC pellet|6.4', 'Uninfected RBC pellet|7.4', 'Uninfected RBC pellet|8.4', 'Parasites pellet|6.4', 'Parasites pellet|7.4', 'Parasites pellet|8.4', 'blank media', 'Infected RBC media|6.4', 'Infected RBC media|7.4', 'Infected RBC media|8.4', 'Uninfected RBC media|6.4', 'Uninfected RBC media|7.4', 'Uninfected RBC media|8.4', 'Parasites media|6.4', 'Parasites media|7.4', 'Parasites media|8.4'];

  my $url = $self->getBaseUrl() . '/a/service/profileSet/Isotopomers/' . $compoundId;
  my $content = get($url);
  my $json = from_json($content);
  my $jsonForService;
  foreach my $result (@$json) {
    my $isotopomer = $result->{'ISOTOPOMER'};
    if ($isotopomer) {
      if (defined $jsonForService) {
        $jsonForService = $jsonForService . ",{\"profileSetName\":\"Profiles of Metabolites from Llinas\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$isotopomer\",\"name\":\"$compoundId|$isotopomer\"}";
      } else {
        $jsonForService = "{\"profileSetName\":\"Profiles of Metabolites from Llinas\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$isotopomer\",\"name\":\"$compoundId|$isotopomer\"}";
      }
    }
  }

  if (!defined $jsonForService) {
    $jsonForService = "{\"profileSetName\":\"Profiles of Metabolites from Llinas\",\"profileType\":\"values\",\"name\":\"$compoundId\"}";
  }
    
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");

  my $massSpec = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::MassSpec->new(@_);
  my $rAdjustString = <<'RADJUST';
    profile.df.full <- profile.df.full[!grepl("blank", profile.df.full$NAME),]
    profile.df.full$LEGEND=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,1];
    profile.df.full$pH=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,2];
    profile.df.full$STACK=matrix(unlist(strsplit(as.character(profile.df.full$DISPLAY_NAME), fixed=T, split=c("|"))), ncol=2, byrow=T) [,2];
    profile.df.full$STACK[which(profile.df.full$STACK == "C12")] = "C12-0";
    profile.df.full$order=matrix(unlist(strsplit(as.character(profile.df.full$STACK), fixed=T, split=c("-"))), ncol=2, byrow=T)[,2];
    profile.df.full$STACK[which(profile.df.full$STACK == "C12-0")] = "C12";
    profile.df.full$NAME = factor(profile.df.full$LEGEND, levels=legend.label);
    profile.df.full$LEGEND = factor(profile.df.full$LEGEND, levels=legend.label);
    profile.df.full <- profile.df.full[order(as.numeric(profile.df.full$order)),]; 
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
  $massSpec->setLegendLabels(['Infected RBC pellet', 'Uninfected RBC pellet', 'Parasites pellet', 'Infected RBC media', 'Uninfected RBC media', 'Parasites media']);

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
