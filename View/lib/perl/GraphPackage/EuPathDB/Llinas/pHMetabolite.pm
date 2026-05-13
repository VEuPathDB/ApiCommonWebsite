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

  my $elementNames = ['Percoll pH 6.4 pellet', 'Percoll pH 6.4 media',
                      'Percoll pH 7.4 pellet', 'Percoll pH 7.4 media',
                      'Percoll pH 8.4 pellet', 'Percoll pH 8.4 media',
                      'RBC pH 6.4 pellet', 'RBC pH 6.4 media',
                      'RBC pH 7.4 pellet', 'RBC pH 7.4 media',
                      'RBC pH 8.4 pellet', 'RBC pH 8.4 media',
                      'Saponin pH 6.4 pellet', 'Saponin pH 6.4 media',
                      'Saponin pH 7.4 pellet', 'Saponin pH 7.4 media',
                      'Saponin pH 8.4 pellet', 'Saponin pH 8.4 media'];

  my $url = $self->getBaseUrl() . '/a/service/profileSet/Isotopomers/' . $compoundId;
  my $content = get($url);
  my $json = from_json($content);
  my $jsonForService;
  foreach my $result (@$json) {
    my $isotopomer = $result->{'isotopomer'};
    if ($isotopomer) {
      if (defined $jsonForService) {
        $jsonForService = $jsonForService . ",{\"profileSetName\":\"Profiles of Metabolites from Llinas [metabolite_massSpec]\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$isotopomer\",\"name\":\"$compoundId|$isotopomer\"}";
      } else {
        $jsonForService = "{\"profileSetName\":\"Profiles of Metabolites from Llinas [metabolite_massSpec]\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$isotopomer\",\"name\":\"$compoundId|$isotopomer\"}";
      }
    }
  }

  if (!defined $jsonForService) {
    $jsonForService = "{\"profileSetName\":\"Profiles of Metabolites from Llinas [metabolite_massSpec]\",\"profileType\":\"values\",\"name\":\"$compoundId\"}";
  }
    
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSets->setJsonForService($jsonForService);
  $profileSets->setSqlName("Profile");

  my $massSpec = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::MassSpec->new(@_);
  my $rAdjustString = <<'RADJUST';
    profile.df.full <- profile.df.full[!grepl("blank", profile.df.full$NAME),]
    profile.df.full$NAME <- gsub(" \\(metabolite_massSpec\\)", "", profile.df.full$NAME)
    profile.df.full$pH <- gsub(".*pH (\\d+\\.\\d+).*", "\\1", profile.df.full$NAME)
    profile.df.full$frac <- ifelse(grepl("media", profile.df.full$NAME), "media", "pellet")
    profile.df.full$group <- gsub(" pH.*", "", profile.df.full$NAME)
    profile.df.full$LEGEND <- paste(profile.df.full$group, profile.df.full$frac)
    profile.df.full$STACK <- gsub(".*\\|", "", profile.df.full$DISPLAY_NAME)
    profile.df.full$STACK[which(profile.df.full$STACK == "C12")] <- "C12-0"
    profile.df.full$order <- gsub(".*-", "", profile.df.full$STACK)
    profile.df.full$STACK[which(profile.df.full$STACK == "C12-0")] <- "C12"
    profile.df.full$NAME <- factor(profile.df.full$LEGEND, levels=legend.label)
    profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels=legend.label)
    profile.df.full <- profile.df.full[order(as.numeric(profile.df.full$order)),]
    profile.df.full$STACK <- factor(profile.df.full$STACK, levels=rev(unique(profile.df.full$STACK)))
    hideLegend <- FALSE
RADJUST
  $massSpec->setAdjustProfile($rAdjustString);
  $massSpec->setProfileSets([$profileSets]);
  $massSpec->setColors($colors);
  $massSpec->setDefaultYMax(100);
  $massSpec->setIsStacked(1);
  $massSpec->setHideXAxisLabels(1);
  $massSpec->setLegendColors($colors);
  $massSpec->setLegendLabels(['Percoll pellet', 'Percoll media', 'RBC pellet', 'RBC media', 'Saponin pellet', 'Saponin media']);

  $self->setGraphObjects($massSpec);
  return $self;
}


1;
