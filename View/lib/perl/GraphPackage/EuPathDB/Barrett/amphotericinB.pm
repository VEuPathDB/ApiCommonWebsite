package ApiCommonWebsite::View::GraphPackage::EuPathDB::Barrett::amphotericinB;

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

  $self->setFacets(["peak"]);
  $self->setPlotWidth(600);

  my $compoundId = $self->getId();

  my $colors = ['dodgerblue', 'slateblue', 'forestgreen', '#2F4F4F', 'salmon'];

  my $elementNames = ['AmBRA-cl1|peak', 'AmBRB-cl2|peak', 'AmBRC-cl3|peak', 'AmBRD-cl2|peak', 'WT|peak'];



  my $url = $self->getBaseUrl() . '/a/service/profileSet/CompoundPeaksIdentifierAmoB/' . $compoundId;
  my $content = get($url);
  my $json = from_json($content);

  my $jsonForService;

  foreach my $result (@$json) {

    my $compoundpeaks = $result->{'COMPOUNDPEAKSIDENTIFIER'};
    
    if ($compoundpeaks) {
      if (defined $jsonForService) {
        $jsonForService = $jsonForService . ",{\"profileSetName\":\"Barrett_AmphotericinB_Resistant [metaboliteProfiles]\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$compoundpeaks\",\"name\":\"$compoundId|$compoundpeaks\"}";
      } else {
        $jsonForService = "{\"profileSetName\":\"Barrett_AmphotericinB_Resistant [metaboliteProfiles]\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$compoundpeaks\",\"name\":\"$compoundId|$compoundpeaks\"}";
      }
    }
  }

  if (!defined $jsonForService) {
    $jsonForService = "{\"profileSetName\":\"Barrett_AmphotericinB_Resistant [metaboliteProfiles]\",\"profileType\":\"values\",\"name\":\"$compoundId\"}";
  }
    
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");

  my $massSpec = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::MassSpec->new(@_);
  my $rAdjustString = <<'RADJUST';
    profile.df.full <- profile.df.full[!grepl("blank", profile.df.full$NAME),]
    profile.df.full$LEGEND=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,1];
    profile.df.full$peak=matrix(unlist(strsplit(as.character(profile.df.full$NAME), fixed=T, split=c("|"))), ncol=2, byrow=T)[,2];
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
  $massSpec->setLegendLabels(['AmBRA-cl1', 'AmBRB-cl2', 'AmBRC-cl3', 'AmBRD-cl2', 'WT']);

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
