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

  $self->setPlotWidth(600);

  my $compoundId = $self->getId();

  my $colors = ['dodgerblue', 'slateblue'];

  my $url = $self->getBaseUrl() . '/a/service/profileSet/CompoundPeaksIdentifier/' . $compoundId;

  my $content = get($url);
  my $json = from_json($content);

  my @graphObjects;

  foreach my $result (@$json) {
    my $jsonForService;
  
    my $compoundpeaks = $result->{'COMPOUNDPEAKSIDENTIFIER'};
    if ($compoundpeaks) {
      $jsonForService = "{\"profileSetName\":\"Barrett_PurineStarvation [metaboliteProfiles]\",\"profileType\":\"values\",\"idOverride\":\"$compoundId|$compoundpeaks\",\"name\":\"$compoundId|$compoundpeaks\"}";
    } else {
      $jsonForService = "{\"profileSetName\":\"Barrett_PurineStarvation [metaboliteProfiles]\",\"profileType\":\"values\",\"name\":\"$compoundId\"}";
    }

    my $profileSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
    $profileSets->setJsonForService($jsonForService);
    $profileSets->setSqlName("Profile");
  
  
    my $massSpec = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::MassSpec->new(@_);
    my $rAdjustString = <<'RADJUST';
      hideLegend <- FALSE;
RADJUST

    $massSpec->setAdjustProfile($rAdjustString);
    $massSpec->setProfileSets([$profileSets]);
    $massSpec->setColors($colors);
    $massSpec->setDefaultYMax(100);
  
    $massSpec->setIsStacked(1);
    $massSpec->setHideXAxisLabels(1);
    $massSpec->setLegendColors($colors);
    $massSpec->setLegendLabels(['Log Growth', 'Purine Starved Growth']);
  
    $massSpec->setPartName("mass_spec_$compoundpeaks");
    my @peakInfo = split('\|', $compoundpeaks); 
    $massSpec->setPlotTitle("Profile - $compoundId; mass - @peakInfo[0]; ret time - @peakInfo[1]");

    push @graphObjects, $massSpec;
  }

  $self->setGraphObjects(@graphObjects);
  return $self;
}

sub declareParts {
  my ($self) = @_;

  my $arrayRef = $self->SUPER::declareParts();
  my $width = @{$arrayRef}[0]->{width};
  my $plotCount = scalar @{$arrayRef};
  my $height = @{$arrayRef}[0]->{height} * $plotCount;

  my $newVisiblePart;
  foreach my $plotPart (@{$arrayRef}) {
    if ($newVisiblePart) {
      $newVisiblePart = $newVisiblePart . "," . $plotPart->{visible_part};
    } else {
      $newVisiblePart = $plotPart->{visible_part};
    }
  }

  my @newParts = ({height => $height, width => $width, visible_part => $newVisiblePart});
  return \@newParts;
}

1;
