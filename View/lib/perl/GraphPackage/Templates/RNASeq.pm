package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;


# @Override
sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;
  my ($groupName) = $self->getGroupNameFromProfileSetName($profileSetName);

  my ($strand) = $profileSetName =~ /\[.+ \- (.+) \- .+ \- /;
  ($strand) = $profileSetName =~ /\[.+ \- (.+) \- / if  (!$strand);

  $groupName = '' if (!$groupName);
  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  die if (!$strand);
  $strand = $strand eq 'unstranded'? ''  :  '_' . $self->getStrandDictionary()->{$strand};
  if ($groupName eq 'Non Unique') {
    $groupName = '';
  } 
  my $mainKey =  "${groupName}_${profileType}${strand}";
  if ($profileType ne 'values' || $profileSetName =~ / \- nonunique\]/ || $groupName || $strand eq '') {
      return([$mainKey])
  }
  # capital letter B in Both so that this graph is sorted after antisense (reverse sort)
  my $bothStrandsKey = "${groupName}_${profileType}_Both_strands";
  return([$mainKey,$bothStrandsKey]);
}


sub switchStrands {
  return 0;
}

# @Override
sub getGroupRegex {
  return qr/\- (.+) \[/;
}

# @Override
# return: htseq unique OR htseq nonunique
sub getRemainderRegex {
  return qr/\[.* \- (\S+)\]$/;
}

sub setStrandDictionaryHash {
    my ($self,$sd) = @_;
    $self->{_strand_dictionary_hash} = $sd;
}

sub getStrandDictionaryHash {
    my $self = shift;
    return $self->{_strand_dictionary_hash};
}

sub getStrandDictionary {
  my $self = shift;
  my $firststrand = 'sense';
  my $secondstrand = 'antisense';

  if ($self->switchStrands()) {
    $firststrand = 'antisense';
    $secondstrand = 'sense';
  }

  my $hash = { 'unstranded' => '',
	   'firststrand' => $firststrand,
	   'secondstrand' => $secondstrand
	 };
  $self->setStrandDictionaryHash($hash);
  return($hash);
}

# @Override
sub sortKeys {
  my ($self, $a_tmp, $b_tmp) = @_;

  my ($a_suffix, $a_type, $a_strand) = split(/\_/, $a_tmp);
  my ($b_suffix, $b_type, $b_strand) = split(/\_/, $b_tmp);

  $a_suffix ="" if !($a_suffix);
  $b_suffix ="" if !($b_suffix);

  return ($b_type cmp $a_type)  || ($a_suffix cmp $b_suffix) || ($b_strand cmp $a_strand);

}

# @Override
sub isExcludedProfileSet {
  my ($self, $psName) = @_;
  my ($strand) = $psName =~ /\[.+ \- (.+) \- /;
  $strand = $self->getStrandDictionary()->{$strand};
  my ($isCufflinks) = ($psName =~/\[cuff \-/)? 1: 0;

  my $val =   $self->SUPER::isExcludedProfileSet($self, $psName);
  if ($val) {
#print STDERR "exclude by super - return 1\n";
    return 1;
  } elsif ($psName =~ /htseq-intersection/){
#print STDERR "exclude intersection: $psName - return 1\n";
    return 1;
  } elsif ($psName =~ /eigengene/){
#print STDERR "exclude intersection: $psName - return 1\n";
    return 1;
  } elsif ($isCufflinks){
    return 1;
  } else {
#print STDERR "$psName - return 0\n";
    return 0;
  }
}

# @Override
sub getProfileColors {
  my ($self) = @_;

  my @colors =  @{$self->getColors()};
  unshift ( @colors, 'gray');
  return \@colors; 
}

# @Override
sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $switchStrands = $self->switchStrands();
  my $rAdjustString = << 'RADJUST';
  if ('NAME' %in% colnames(profile.df.full) & 'LEGEND' %in% colnames(profile.df.full)) {
    newVals <- aggregate(VALUE ~ NAME, with(profile.df.full, data.frame(NAME=NAME, VALUE=ifelse(LEGEND=="nonunique", 1, -1)*VALUE)), sum);
    profile.df.full$VALUE[profile.df.full$LEGEND == "nonunique" & profile.df.full$NAME %in% newVals$NAME] <- newVals$VALUE;
    profile.df.full$VALUE[profile.df.full$VALUE < 0] <- 0;
    profile.df.full$STACK <- paste0(profile.df.full$NAME, "- ", profile.df.full$LEGEND, " reads");
    profile.df.full$STDERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MAX_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MIN_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    if (any(c('unique', 'nonunique') %in% profile.df.full$LEGEND)) {
      profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels=c('nonunique', 'unique'))
    }
  }

RADJUST

  $profile->addAdjustProfile($rAdjustString);

}

1;

#vb - gmorYale_SRP093552_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_2be66219b5;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
profile.df.full$NAME <- gsub("Proboscis_from__male_G__m__morsitans_control", "Control", profile.df.full$NAME)
profile.df.full$NAME <- gsub("Proboscis_from__male_G__m__morsitans_infected_with_Trypanosome_congolense", "Infected", profile.df.full$NAME)

RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

#vb - 
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_c0ebe602ad;
sub init{
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(900);

  return $self;
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_1cedd4bd46;
sub init{
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(900);

  return $self;
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_24f2db6f66;
sub init{
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(900);

  return $self;
}
1;

#fungi 
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_9a20a7bfa0;
sub init{
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(700);

  return $self;
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_7ce8a791f1;
sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  return $self;
}
1;

#fungi - ncraOR74A_Ncrassa_carbon_comp_Wu_2021_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_bb6e5ae0e5;
sub init{
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  return $self;
}
1;

#fungi - hcapG217B_Rodriguez_Hcap_Trans_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_39a9b940c0;
sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  return $self;
}



#plasmo, unidb
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_a966e260dd;
sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  return $self;
}

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /iterativeWGCNA Dual transcriptomes of malaria-infected Gambian children/){
    return 1;
  }
  return 0;
}

sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;
  my ($groupName) = $self->getGroupNameFromProfileSetName($profileSetName);

  my ($strand) = $profileSetName =~ /\[.+ \- (.+) \- .+ \- /;
  ($strand) = $profileSetName =~ /\[.+ \- (.+) \- / if  (!$strand);

  $groupName = '' if (!$groupName);
  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  if (!$strand) {return ''};
  $strand = '_' . $self->getStrandDictionary()->{$strand};
  if ($groupName eq 'Non Unique') {
    $groupName = '';
  }
  my $mainKey =  "${groupName}_${profileType}${strand}";
  if ($profileType ne 'values' || $profileSetName =~ / \- nonunique\]/ || $groupName || $strand eq '') {
      return([$mainKey])
  }
  # capital letter B in Both so that this graph is sorted after antisense (reverse sort)
   my $bothStrandsKey = "${groupName}_${profileType}_Both_strands";
   return([$mainKey,$bothStrandsKey]);
}
1;

#vectorbase
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_9800ad244a;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
profile.df.full$NAME <- gsub("Larvae_resistant_to_", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub("_strain", "", profile.df.full$NAME)

RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

#plasmo
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_416070059c;

sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;
  my ($groupName) = $self->getGroupNameFromProfileSetName($profileSetName);

  my ($strand) = $profileSetName =~ /\[.+ \- (.+) \- .+ \- /;
  ($strand) = $profileSetName =~ /\[.+ \- (.+) \- / if  (!$strand);

  $groupName = '' if (!$groupName);
  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  die if (!$strand);
  $strand = $strand eq 'unstranded'? ''  :  '_' . $self->getStrandDictionary()->{$strand};
  if ($groupName eq 'Non Unique') {
    $groupName = '';
  } 
  my $mainKey =  "${groupName}_${profileType}${strand}";
  if ($profileType ne 'values' || $profileSetName =~ / \- nonunique\]/ || $groupName || $strand eq '') {
      return([$mainKey])
  }
  return([$mainKey])
}

sub getGroupRegex {
 return qr/DAFT/;
}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  if ("LEGEND" %in% names(profile.df.full)) {
    profile.df.full <- profile.df.full[profile.df.full$LEGEND == 'unique',]
    profile.df.full$LEGEND <- as.character(profile.df.full$LEGEND)
  }
  profile.df.full$LEGEND[grepl('3D7', profile.df.full$ELEMENT_NAMES)] <- '3D7'
  profile.df.full$LEGEND[grepl('HB3', profile.df.full$ELEMENT_NAMES)] <- 'HB3'
  profile.df.full$LEGEND[grepl('IT', profile.df.full$ELEMENT_NAMES)] <- 'IT'
  profile.df.full$ELEMENT_NAMES <- gsub(' 3D7 DAFT-seq', '', profile.df.full$ELEMENT_NAMES, fixed=T)
  profile.df.full$ELEMENT_NAMES <- gsub(' HB3 DAFT-seq', '', profile.df.full$ELEMENT_NAMES, fixed=T)
  profile.df.full$ELEMENT_NAMES <- gsub(' IT DAFT-seq', '', profile.df.full$ELEMENT_NAMES, fixed=T)
  profile.df.full$ELEMENT_NAMES_NUMERIC = as.numeric(gsub(" *[a-z-A-Z()+-]+ *", "", profile.df.full$ELEMENT_NAMES, perl=T))
  profile.df.full$GROUP <- profile.df.full$LEGEND
RADJUST

  $profile->addAdjustProfile($rAdjustString);
  $profile->setSmoothLines(0);
  $profile->setXaxisLabel("Hours");
}

sub getProfileColors {
  my ($self) = @_;

  my @colors =  @{$self->getColors()};
  unshift ( @colors);
  return \@colors;
}
1;

#vectorbase
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_6ecf1af578;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
profile.df.full$NAME <- gsub("A. gambiae G3 ", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub(" BR", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub("fat body and abdo wall", "FB-enriched abdominal wall", profile.df.full$NAME)

RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

#vectorbase
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_827aa45cff;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
profile.df.full$NAME <- gsub("A. gambiae G3 ", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub(" BR", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub("fat body and abdo wall", "FB-enriched abdominal wall", profile.df.full$NAME)

RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

#vectorbase
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_a92b4db713;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
profile.df.full$NAME <- gsub("A. gambiae G3 ", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub(" BR", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub("fat body and abdo wall", "FB-enriched abdominal wall", profile.df.full$NAME)

RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_cb6310bab8;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
profile.df.full <- profile.df.full[!grepl("hr_after_laying_eggs", profile.df.full$NAME),]

RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_f101fb2669;

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /nonunique/){
    return 1;
  }
  return 0;
}

sub getProfileColors {
  return ['#8F006B'];
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_6393f11883;

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /nonunique/){
    print STDERR "found one to exclue";
    return 1;
  }
  return 0;
}

sub getProfileColors {
  return ['#8F006B'];
}
1;

#host
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_6abf688526;

sub init {
  my $self = shift;
  use Data::Dumper; 

  $self->SUPER::init(@_);

  my $profileSet = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSet->setJsonForService("{\"profileSetName\":\"Parasitemia over 100 days in five mice\",\"profileType\":\"values\",\"idOverride\":\"RMe14\",\"name\":\"RMe14\"},{\"profileSetName\":\"Parasitemia over 100 days in five mice\",\"profileType\":\"values\",\"idOverride\":\"RFv13\",\"name\":\"RFv13\"},{\"profileSetName\":\"Parasitemia over 100 days in five mice\",\"profileType\":\"values\",\"idOverride\":\"RIc14\",\"name\":\"RIc14\"},{\"profileSetName\":\"Parasitemia over 100 days in five mice\",\"profileType\":\"values\",\"idOverride\":\"RSb14\",\"name\":\"RSb14\"},{\"profileSetName\":\"Parasitemia over 100 days in five mice\",\"profileType\":\"values\",\"idOverride\":\"RFa14\",\"name\":\"RFa14\"}");
  $profileSet->setSqlName("Profile");

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  #print STDERR Dumper($line);
  $line->setPartName('parasitemia');
  $line->setProfileSets([$profileSet]);
  $line->setYaxisLabel("Log 10 Parasites / uL");
  $line->setXaxisLabel("Day");
  $line->setPlotTitle("Parasitemia Summary - 100 Days");
  
  my $rAdjustString = << 'RADJUST';

  profile.df.full$VALUE <- log10(profile.df.full$VALUE)
  profile.df.full$VALUE[is.infinite(profile.df.full$VALUE)] <- 0
  profile.df.full$FACET <- as.factor(profile.df.full$DISPLAY_NAME) 
  profile.df.full$PROFILE_SET <- profile.df.full$FACET
  profile.df.full$TOOLTIP <- NA
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 21'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 21'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 20'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 18'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES == 'Day 18'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES == 'Day 22'] <- "TP3 - Euathanasia"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 89'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 91'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 89'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 91'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$hold <- profile.df.full$TOOLTIP
  profile.df.full$TOOLTIP <- paste0("x: " , profile.df.full$ELEMENT_NAMES_NUMERIC, ", y: ", profile.df.full$VALUE, "|Time point: ", profile.df.full$TOOLTIP)
  profile.df.full$TOOLTIP[is.na(profile.df.full$hold)] <- NA
  profile.df.full$hold <- NULL
  profile.df.full$LEGEND <- "Sample\nResults\nNot Loaded"
  profile.df.full$LEGEND[!is.na(profile.df.full$TOOLTIP)] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND <- as.factor(profile.df.full$LEGEND)

RADJUST

my $post = "
gp = gp + theme(legend.key = element_rect(fill = \"white\"),
                legend.key.height = unit(3, \"cm\"))
";

  $line->addAdjustProfile($rAdjustString);
  $line->setFacetNumCols(1);
  $line->setDefaultXMin(0);
  $line->setDefaultXMax(100);
  $line->setColors(["red", "black"]);
  $line->setRPostscript($post);
  $line->setColorPointsOnly(1);
  $line->setScreenSize(500);
  #$line->setLineColors(["black"]);

  my $graphObjects = $self->getGraphObjects();
  push @$graphObjects, $line;
  $self->setGraphObjects(@$graphObjects);

}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';

  profile.df.full <- separate(profile.df.full, ELEMENT_NAMES, c("GROUP", "ELEMENT_NAMES", "TP"), " ")   
  profile.df.full <- separate(profile.df.full, GROUP, c("GROUP", "FACET"), "_")
  profile.df.full$ELEMENT_NAMES <- gsub("Day", "", profile.df.full$ELEMENT_NAMES)
  profile.df.full$FACET <- as.factor(profile.df.full$FACET)
  profile.df.full <- profile.df.full[profile.df.full$GROUP != 'CF97',]
  profile.df.full$GROUP <- as.factor(profile.df.full$GROUP)
  levels(profile.df.full$GROUP) <- c("Bone Marrow", "Whole Blood")
  profile.df.full$LEGEND <- as.factor(profile.df.full$GROUP)
  profile.df.full$ELEMENT_NAMES_NUMERIC <- as.numeric(profile.df.full$ELEMENT_NAMES)
  profile.df.full$TP[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES_NUMERIC == 23] <- "TP3 - Euathanasia"
  profile.df.full$TOOLTIP <- paste0("x: ", profile.df.full$ELEMENT_NAMES_NUMERIC, ", y: ", profile.df.full$VALUE, "|Time point: ", profile.df.full$TP)
  profile.is.numeric <- TRUE

  hideLegend=FALSE;
  expandColors=FALSE;

RADJUST

  $profile->addAdjustProfile($rAdjustString);

  $profile->setColors(["#5DC863FF", "#3B528BFF"]);
  $profile->setXaxisLabel("Day");
  $profile->setFacetNumCols(1);
  $profile->setSmoothLines(0);
  $profile->setForceNoLines(1);
  $profile->setScreenSize(500);
  $profile->setDefaultXMin(0);
  $profile->setDefaultXMax(100);
}

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /nonunique/){
    return 1;
  }
  return 0;
}

sub declareParts {
  my ($self) = @_;

  my $arrayRef = $self->SUPER::declareParts();
  foreach my $plotPart (@{$arrayRef}) {
    if ($plotPart->{visible_part} ne 'parasitemia') {
      $plotPart->{visible_part} = $plotPart->{visible_part} . ",parasitemia";
      $plotPart->{height} = $plotPart->{height} + 500;
    }
  }
  print STDERR Dumper($arrayRef);
  return $arrayRef;
}

1;

#plasmo
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_a2967e5664;

sub init {
  my $self = shift;
  use Data::Dumper; 

  $self->SUPER::init(@_);

  my $profileSet = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $profileSet->setJsonForService("{\"profileSetName\":\"Parasitemia over 100 days in five monkeys\",\"profileType\":\"values\",\"idOverride\":\"RMe14\",\"name\":\"RMe14\"},{\"profileSetName\":\"Parasitemia over 100 days in five monkeys\",\"profileType\":\"values\",\"idOverride\":\"RFv13\",\"name\":\"RFv13\"},{\"profileSetName\":\"Parasitemia over 100 days in five monkeys\",\"profileType\":\"values\",\"idOverride\":\"RIc14\",\"name\":\"RIc14\"},{\"profileSetName\":\"Parasitemia over 100 days in five monkeys\",\"profileType\":\"values\",\"idOverride\":\"RSb14\",\"name\":\"RSb14\"},{\"profileSetName\":\"Parasitemia over 100 days in five monkeys\",\"profileType\":\"values\",\"idOverride\":\"RFa14\",\"name\":\"RFa14\"}");
   $profileSet->setSqlName("Profile");

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  #print STDERR Dumper($line);
  $line->setPartName('parasitemia');
  $line->setProfileSets([$profileSet]);
  $line->setYaxisLabel("Log 10 Parasites / uL");
  $line->setXaxisLabel("Day");
  $line->setPlotTitle("Parasitemia Summary - 100 Days");
  
  my $rAdjustString = << 'RADJUST';

  profile.df.full$VALUE <- log10(profile.df.full$VALUE)
  profile.df.full$VALUE[is.infinite(profile.df.full$VALUE)] <- 0
  profile.df.full$FACET <- as.factor(profile.df.full$DISPLAY_NAME) 
  profile.df.full$PROFILE_SET <- profile.df.full$FACET
  profile.df.full$LEGEND <- "Sample\nResults\nNot Loaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 21'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 21'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 20'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 18'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES == 'Day 18'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 91'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "Sample\nResults\nLoaded"
  profile.df.full$LEGEND <- as.factor(profile.df.full$LEGEND)
  profile.df.full$TOOLTIP <- NA
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES == 'Day 1'] <- "TP1"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 21'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 21'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 20'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 18'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES == 'Day 18'] <- "TP2"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 26'] <- "TP3"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFv13' & profile.df.full$ELEMENT_NAMES == 'Day 22'] <- "TP3 - Euathanasia"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 63'] <- "TP4"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 76'] <- "TP5"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 89'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 91'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 89'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 91'] <- "TP6"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RIc14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RSb14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RMe14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$TOOLTIP[profile.df.full$FACET == 'RFa14' & profile.df.full$ELEMENT_NAMES == 'Day 97'] <- "TP7"
  profile.df.full$hold <- profile.df.full$TOOLTIP
  profile.df.full$TOOLTIP <- paste0("x: " , profile.df.full$ELEMENT_NAMES_NUMERIC, ", y: ", profile.df.full$VALUE, "|Time point: ", profile.df.full$TOOLTIP)
  profile.df.full$TOOLTIP[is.na(profile.df.full$hold)] <- NA
  profile.df.full$hold <- NULL

RADJUST

my $post = "
gp = gp + theme(legend.key = element_rect(fill = \"white\"),
                legend.key.height = unit(3, \"cm\"))
";

  $line->addAdjustProfile($rAdjustString);
  $line->setFacetNumCols(1);
  $line->setDefaultXMin(0);
  $line->setDefaultXMax(100);
  $line->setColors(["red", "black"]);
  $line->setRPostscript($post);
  $line->setColorPointsOnly(1);
  $line->setScreenSize(500);
  #$line->setLineColors(["black"]);

  my $graphObjects = $self->getGraphObjects();
  push @$graphObjects, $line;
  $self->setGraphObjects(@$graphObjects);

}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';

  profile.df.full <- separate(profile.df.full, ELEMENT_NAMES, c("GROUP", "ELEMENT_NAMES", "TP"), " ")   
  profile.df.full <- separate(profile.df.full, GROUP, c("GROUP", "trash2", "FACET"), "_")
  profile.df.full$trash2 <- NULL
  profile.df.full$ELEMENT_NAMES <- gsub("Day", "", profile.df.full$ELEMENT_NAMES)
  profile.df.full$FACET <- as.factor(profile.df.full$FACET)
  profile.df.full$GROUP <- as.factor(profile.df.full$GROUP)
  levels(profile.df.full$GROUP) <- c("Bone Marrow", "Whole Blood")
  profile.df.full$LEGEND <- as.factor(profile.df.full$GROUP)
  profile.df.full$ELEMENT_NAMES_NUMERIC <- as.numeric(profile.df.full$ELEMENT_NAMES)
  profile.df.full$TOOLTIP <- paste0("x: ", profile.df.full$ELEMENT_NAMES_NUMERIC, ", y: ", profile.df.full$VALUE, "|Time point: ", profile.df.full$TP)
  profile.is.numeric <- TRUE

  hideLegend=FALSE;
  expandColors=FALSE;

RADJUST

  $profile->addAdjustProfile($rAdjustString);

  $profile->setColors(["#5DC863FF", "#3B528BFF"]);
  $profile->setXaxisLabel("Day");
  $profile->setFacetNumCols(1);
  $profile->setSmoothLines(0);
  $profile->setForceNoLines(1);
  $profile->setScreenSize(500);
  $profile->setDefaultXMin(0);
  $profile->setDefaultXMax(100);
}

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /nonunique/){
    return 1;
  }
  return 0;
}

sub declareParts {
  my ($self) = @_;

  my $arrayRef = $self->SUPER::declareParts();
  foreach my $plotPart (@{$arrayRef}) {
    if ($plotPart->{visible_part} ne 'parasitemia') {
      $plotPart->{visible_part} = $plotPart->{visible_part} . ",parasitemia";
      $plotPart->{height} = $plotPart->{height} + 500;
    }
  }
  print STDERR Dumper($arrayRef);
  return $arrayRef;
}

1;

#fungi 
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_a2d28b5866;
sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  return $self;
}

1;

#fungi treeQM6a_Benocci_Plant_Biomass_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_de2b36b6b9;
sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(800);

  return $self;
}

1;


# ToxoDB tgonME49_Saeij_Jeroen_strains_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_485e6e94e3;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(700);

  return $self;
}

1;

# HostDB mmusC57BL6J_Saeij_Jeroen_strains_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_b8755b3393;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(700);

  return $self;
}

1;

# FungiDB ncraOR74A_Ncrassa_plantbiomass_Wu_2021_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_1dd4f50bad;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  $self->setPlotWidth(700);

  return $self;
}

1;


# Fungi 
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_eb1f1d3a5c;

# @Override
sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;

  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  my $metacycle = 0;
  if (index($profileSetName, "time course") == 0) {
    $metacycle = 1;
  } 
  my $mainKey =  ["_${profileType}"];

  #trying to ignore dup values, but also pass everything else to same plot part as the main profile to use as annotation
  if ($metacycle) {
    if ($profileType eq 'values' || $profileType eq 'percentile') {
      return([]);
    } else {
      $mainKey = ["_values", "_percentile"];
    }
  }

  return($mainKey);
}

sub getProfileColors {
  my ($self) = @_;

  my @colors =  @{$self->getColors()};
  return \@colors;
}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  annotation.df <- profile.df.full[!profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),]
  profile.df.full <- profile.df.full[profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),] 
  metadata.df <- as.data.frame(matrix(unlist(strsplit(as.character(profile.df.full$ELEMENT_NAMES), '_', fixed=T)), ncol = 3, byrow=T))
  profile.df.full$ELEMENT_NAMES_NUMERIC <- as.numeric(gsub('DD', '', metadata.df[[2]]))
  profile.df.full$GROUP <- gsub("TM", "Time series ", metadata.df[[1]])
  profile.df.full$TOOLTIP <- gsub("CT", "Circadian time: ", metadata.df[[3]])
  profile.df.full$GROUP <- ifelse(grepl('firststrand', profile.df.full$PROFILE_SET), paste(profile.df.full$GROUP, 'sense'), paste(profile.df.full$GROUP, 'antisense'))
  profile.df.full$LEGEND <- as.factor(profile.df.full$GROUP)
  profile.df.full <- profile.df.full[ !grepl('nonunique', profile.df.full$PROFILE_SET) , ]

  if (nrow(annotation.df) > 0) {  
    annotation.df <- transform(annotation.df, "VALUE"=ifelse(VALUE < .05 & PROFILE_TYPE == "pvalue", "<0.05", VALUE)) 
    annotation.df$GROUP <- gsub("\\s-(.*)", "", annotation.df$PROFILE_SET)
    annotation.df$GROUP <- gsub('time course', 'Time series', annotation.df$GROUP)
    annotation.df$LEGEND <- annotation.df$GROUP
    annotation.df$LINETEXT <- paste0(substr(annotation.df$ELEMENT_NAMES, 1,3), " ", annotation.df$PROFILE_TYPE, ": ", annotation.df$VALUE)
    annotation.df <- group_by(annotation.df, LEGEND)
    annotation.df <- summarize(annotation.df, LINETEXT = paste(LINETEXT, collapse="||"))
    annotation.df$LINETEXT <- paste0(annotation.df$LEGEND, "||", annotation.df$LINETEXT)
    profile.df.full <- merge(profile.df.full, annotation.df, by = "LEGEND")
  }

  profile.is.numeric <- TRUE
RADJUST

  my $rPostscript = << 'RPOST';
if ("LINETEXT" %in% colnames(profile.df.full) && useTooltips) {  
    remove_geom <- function(ggplot2_object, geom_type) {
      layers <- lapply(ggplot2_object$layers, 
        function(x) {
          if (class(x$geom)[1] == geom_type) {
            NULL
          } else {
            x
          }
        }
      )
      layers <- layers[!sapply(layers, is.null)]
      ggplot2_object$layers <- layers
      ggplot2_object
    }
  
    gp <- remove_geom(gp, "GeomLine")
 
    gp = gp + aes(group=GROUP)
    gp = gp + geom_tooltip(aes(tooltip=LINETEXT), real.geom=geom_line)
  }
RPOST

  $profile->setSmoothLines(0);
  $profile->setXaxisLabel('Hours after light to dark transfer');
  $profile->addAdjustProfile($rAdjustString);
  $profile->setRPostscript($rPostscript);
}

1;


# Fungi - ncraOR74A_Bharath_Circadian_Time_Course_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_c05fd37f3c;

# @Override
sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;

  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  my $metacycle = 0;
  if (index($profileSetName, "MetaCycle") != -1) {
    $metacycle = 1;
  } 
  my $mainKey =  ["_${profileType}"];

  #trying to ignore dup values, but also pass everything else to same plot part as the main profile to use as annotation
  if ($metacycle) {
    if ($profileType eq 'values' || $profileType eq 'percentile') {
      return([]);
    } else {
      $mainKey = ["_values", "_percentile"];
    }
  }

  return($mainKey);
}

sub getProfileColors {
  my ($self) = @_;

  my @colors =  @{$self->getColors()};
  return \@colors;
}

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /RNA-sequencing of WT and ΔMSN1 strains of N. crassa over a circadian time course - / || $psName =~ /nonunique/){
    return 1;
  }
  return 0;
}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  annotation.df <- profile.df.full[!profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),]
  profile.df.full <- profile.df.full[profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),]
  profile.df.full$GROUP <- NA
  profile.df.full$GROUP[grepl('firststrand', profile.df.full$PROFILE_SET) & grepl('wt ', profile.df.full$ELEMENT_NAMES)] <- 'WT sense strand'
  profile.df.full$GROUP[grepl('secondstrand', profile.df.full$PROFILE_SET) & grepl('wt ', profile.df.full$ELEMENT_NAMES)] <- 'WT antisense strand'
  profile.df.full$GROUP[grepl('firststrand', profile.df.full$PROFILE_SET) & grepl('MSN1 ', profile.df.full$ELEMENT_NAMES)] <- 'MSN sense strand'
  profile.df.full$GROUP[grepl('secondstrand', profile.df.full$PROFILE_SET) & grepl('MSN1 ', profile.df.full$ELEMENT_NAMES)] <- 'MSN antisense strand'
  profile.df.full$LEGEND = profile.df.full$GROUP


  if (nrow(annotation.df) > 0) {  
    annotation.df <- transform(annotation.df, "VALUE"=ifelse(VALUE < .05 & PROFILE_TYPE == "pvalue", "<0.05", VALUE)) 
    annotation.df$GROUP <- gsub("\\s-(.*)", "", annotation.df$PROFILE_SET)
    annotation.df$GROUP <- gsub(' MetaCycle', '', annotation.df$GROUP)
    annotation.df$GROUP <- gsub('FirstStrand', 'sense strand', annotation.df$GROUP)
    annotation.df$GROUP <- gsub('SecondStrand', 'antisense strand', annotation.df$GROUP)
    annotation.df$LEGEND <- annotation.df$GROUP
    annotation.df$LINETEXT <- paste0(substr(annotation.df$ELEMENT_NAMES, 1,3), " ", annotation.df$PROFILE_TYPE, ": ", annotation.df$VALUE)
    annotation.df <- group_by(annotation.df, LEGEND)
    annotation.df <- summarize(annotation.df, LINETEXT = paste(LINETEXT, collapse="||"))
    annotation.df$LINETEXT <- paste0(annotation.df$LEGEND, "||", annotation.df$LINETEXT)
    profile.df.full <- merge(profile.df.full, annotation.df, by = "LEGEND")
  }

  profile.is.numeric <- TRUE
RADJUST

  my $rPostscript = << 'RPOST';
  if ("LINETEXT" %in% colnames(profile.df.full) & useTooltips) {  
    remove_geom <- function(ggplot2_object, geom_type) {
      layers <- lapply(ggplot2_object$layers, 
        function(x) {
          if (class(x$geom)[1] == geom_type) {
   	    NULL
    	  } else {
    	    x
   	  }
        }
      )
      layers <- layers[!sapply(layers, is.null)]
      ggplot2_object$layers <- layers
      ggplot2_object
    }
  
    gp <- remove_geom(gp, "GeomLine")
 
    gp = gp + aes(group=GROUP)
    gp = gp + geom_tooltip(aes(tooltip=LINETEXT), real.geom=geom_line)
  }
RPOST

  $profile->setSmoothLines(0);
  $profile->setXaxisLabel('Hours');
  $profile->addAdjustProfile($rAdjustString);
  $profile->setRPostscript($rPostscript);
}

1;

# FungiDB calbSC5314_Brown_Calbicans_2015_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_79549674a3;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);
  $self->setPlotWidth(600);
  return $self;
}

1;

# FungiDB mory70-15_Oses-Ruiz_Mory_2020_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_db10ef088b;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);
  $self->setPlotWidth(600);
  return $self;
}

1;

# FungiDB cneoH99_YU_Cneo_CNS_2021_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_c7fd03bc1e;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);
  $self->setPlotWidth(2000);
  return $self;
}

1;



# TriTryp - tbruTREU927_Rijo_Circadian_Regulation_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_77b994d105;
use Data::Dumper;

# @Override
sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;

  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  my $metacycle = 0;
  if (index($profileSetName, "MetaCycle") != -1) {
    $metacycle = 1;
  } 
  my $mainKey =  ["_${profileType}"];

  #trying to ignore dup values, but also pass everything else to same plot part as the main profile to use as annotation
  if ($metacycle) {
    if ($profileType eq 'values' || $profileType eq 'percentile') {
      return([]);
    } else {
      $mainKey = ["_values", "_percentile"];
    }
  }

  return($mainKey);
}

sub getProfileColors {
  my ($self) = @_;

  my @colors =  @{$self->getColors()};
  return \@colors;
}

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /Circadian Control of Bloodstream and Procyclic Form Transcriptomes - / || $psName =~ /nonunique/){
    return 1;
  }
  return 0;
}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  annotation.df <- profile.df.full[!profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),]
  profile.df.full <- profile.df.full[profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),]
  profile.df.full$GROUP <- gsub("([0-9]+)...", "", profile.df.full$ELEMENT_NAMES)
  profile.df.full$PROFILE_SET = profile.df.full$GROUP
  profile.df.full$LEGEND = profile.df.full$GROUP

  if (nrow(annotation.df) > 0) {  
    annotation.df <- transform(annotation.df, "VALUE"=ifelse(VALUE < .05 & PROFILE_TYPE == "pvalue", "<0.05", VALUE)) 
    annotation.df$LEGEND <- gsub("  ", " ", paste(gsub("Con", "Const", gsub("Cons", "Con", gsub("_", " ", substr(annotation.df$ELEMENT_NAMES, 11,17)))), "Temp"))
    annotation.df$LINETEXT <- paste0(substr(annotation.df$ELEMENT_NAMES, 1,3), " ", annotation.df$PROFILE_TYPE, ": ", annotation.df$VALUE)
    annotation.df <- group_by(annotation.df, LEGEND)
    annotation.df <- summarize(annotation.df, LINETEXT = paste(LINETEXT, collapse="||"))
    annotation.df$LINETEXT <- paste0(annotation.df$LEGEND, "||", annotation.df$LINETEXT)
    profile.df.full <- merge(profile.df.full, annotation.df, by = "LEGEND")
  }
RADJUST

  my $rPostscript = << 'RPOST';
  if ("LINETEXT" %in% colnames(profile.df.full) & useTooltips) {  
    remove_geom <- function(ggplot2_object, geom_type) {
      layers <- lapply(ggplot2_object$layers, 
        function(x) {
          if (class(x$geom)[1] == geom_type) {
   	    NULL
    	  } else {
    	    x
   	  }
        }
      )
      layers <- layers[!sapply(layers, is.null)]
      ggplot2_object$layers <- layers
      ggplot2_object
    }
  
    gp <- remove_geom(gp, "GeomLine")
 
    gp = gp + aes(group=GROUP)
    gp = gp + geom_tooltip(aes(tooltip=LINETEXT), real.geom=geom_line)
  }
RPOST

  $profile->setSmoothLines(0);
  $profile->setXaxisLabel('Hours');
  $profile->addAdjustProfile($rAdjustString);
  $profile->setRPostscript($rPostscript);
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_b0427cd47b;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $legend = ['acute infection ', 'chronic infection '];
  $profile->setSampleLabels($legend);
 
  my $rAdjustString = << 'RADJUST';
  if ('NAME' %in% colnames(profile.df.full) & 'LEGEND' %in% colnames(profile.df.full)) {
    newVals <- aggregate(VALUE ~ NAME, with(profile.df.full, data.frame(NAME=NAME, VALUE=ifelse(LEGEND=="nonunique", 1, -1)*VALUE)), sum);
    profile.df.full$VALUE[profile.df.full$LEGEND == "nonunique" & profile.df.full$NAME %in% newVals$NAME] <- newVals$VALUE;
    profile.df.full$VALUE[profile.df.full$VALUE < 0] <- 0;
    profile.df.full$STACK <- paste0(profile.df.full$NAME, "- ", profile.df.full$LEGEND, " reads");
    #profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels = rev(levels(as.factor(profile.df.full$LEGEND))));
    profile.df.full$STDERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MAX_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MIN_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA

    if (any(c('unique', 'nonunique') %in% profile.df.full$LEGEND)) {
      profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels=c('nonunique', 'unique'))
    }
  }
RADJUST

  $profile->addAdjustProfile($rAdjustString);

}
1;

# tbruTREU927_RNAi_Horn_*rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_98fb258539;
use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::ProfileSet;
use EbrcWebsiteCommon::Model::CannedQuery::RankedNthRatioValues;
use EbrcWebsiteCommon::Model::CannedQuery::PhenotypeRankedNthNames;
use Data::Dumper;

# @Override
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $specs = $self->getSpecs();
  my $id = $self->getId();

  my @profileSets;
  foreach my $ps (@$specs) {
    my @profileSet = $self->makeProfileSets($ps->{query}, $ps->{abbrev}, $ps->{name});
    push @profileSets, @profileSet;
  }
  my $go = EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot->new(@_);

  $go->setProfileSets(\@profileSets);
  $go->setXaxisLabel("");
  $go->setPartName("ratio.rank");
  $go->setPlotTitle("$id - Unique CDS aligned");
  $go->setYaxisLabel("tpm ratio (relative fitness)");
  $go->setColors(["gray", "red"]);
#  $go->setColors(["#4f5051", "#6d6e70", "#c0c1c4", "#909193", "red"]);
  $go->addAdjustProfile('
profile.df.full$NUM <- round(profile.df.full$NUM, digits = 2)
profile.df.full$DENOM <- round(profile.df.full$DENOM, digits = 2)
profile.df.full$TOOLTIP <- paste0("Ratio ", profile.df.full$NUM, " : ", profile.df.full$DENOM)
#profile.df.full$TOOLTIP <- paste0(profile.df.full$NUM, " : ", profile.df.full$DENOM, "|", profile.df.full$PROFILE_SET)
profile.df.full$FACET <- as.factor(paste0(unlist(lapply(strsplit(profile.df.full$PROFILE_SET, " - "),"[",1)), " / NoTet"))
profile.df.full$LEGEND[!grepl("ALL", profile.df.full$PROFILE_SET)] <- unlist(lapply(strsplit(profile.df.full$PROFILE_SET[!grepl("ALL", profile.df.full$PROFILE_SET)], " - "),"[",2))
profile.df.full$LEGEND[grepl("ALL", profile.df.full$PROFILE_SET)] <- "All Genes"
profile.df.gene <- profile.df.full[!(grepl("NoTet", profile.df.full$LEGEND)),]
profile.df.full <- profile.df.full[grepl("NoTet", profile.df.full$LEGEND),]
profile.df.full <- rbind(profile.df.full, profile.df.gene)
  ');
  $go->setRPostscript("
gp = gp + scale_y_log10() 
gp = gp + scale_x_continuous(breaks = round(seq(min(profile.df.full\$ELEMENT_NAMES_NUMERIC), max(profile.df.full\$ELEMENT_NAMES_NUMERIC), by = 11000),1))
gp = gp + geom_hline(yintercept=1, color=\"darkgray\", size=0.5)
");
  $go->setFacetNumCols(4);

  $self->setGraphObjects($go);

  return $self;
}

sub getSpecs {
  return[ {abbrev => "BFD3",
	   name => "BFD3",
	   query => "select bfd3.source_id,  bfd3.value/notet.value as value, bfd3.value as num, notet.value as denom from (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%BFD3%') bfd3 left join (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%Tet%') notet on bfd3.source_id = notet.source_id"},
	  {abbrev => "BFD6",
	   name => "BFD6",
	   query => "select bfd3.source_id,  bfd3.value/notet.value as value, bfd3.value as num, notet.value as denom from (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%BFD6%') bfd3 left join (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%Tet%') notet on bfd3.source_id = notet.source_id"},
	  {abbrev => "PF",
	   name => "PF",
	   query => "select bfd3.source_id,  bfd3.value/notet.value as value, bfd3.value as num, notet.value as denom from (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%PF%') bfd3 left join (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%Tet%') notet on bfd3.source_id = notet.source_id"},
	  {abbrev => "DIF",
	   name => "DIF",
	   query => "select bfd3.source_id,  bfd3.value/notet.value as value, bfd3.value as num, notet.value as denom from (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%DIF%') bfd3 left join (SELECT ga.source_id, CASE WHEN (nafe.value = 0) THEN 0.019 ELSE nafe.value END as value FROM webready.GeneAttributes_p ga, results.nafeatureexpression nafe, study.protocolappnode pan, study.studylink sl, study.study s WHERE nafe.na_feature_id = ga.na_feature_id AND pan.protocol_app_node_id = sl.protocol_app_node_id AND nafe.protocol_app_node_id = sl.protocol_app_node_id AND sl.study_id = s.study_id AND s.NAME = 'T.brucei paired end RNA Seq data from Horn aligned with cds coordinates [htseq-union - unstranded - tpm - unique]' AND pan.NAME LIKE '%Tet%') notet on bfd3.source_id = notet.source_id"} ];
}

sub makeProfileSets {
  my ($self, $sourceIdValueQuery, $abbrev, $name) = @_;

  my $id = $self->getId();

  my $goProfileSetCurve = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $goProfileSetCurve->setJsonForService("{\"sourceIdValueQuery\":\"$sourceIdValueQuery\",\"N\":\"100\",\"idOverride\":\"ALL\",\"name\":\"$name - ALL\"}");
  $goProfileSetCurve->setSqlName("RankedNthRatioValues");

  my $goProfileSetGene = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
  $goProfileSetGene->setJsonForService("{\"sourceIdValueQuery\":\"$sourceIdValueQuery\",\"N\":\"100\",\"name\":\"$name - $id\"}");
  $goProfileSetGene->setProfileNamesCannedQuery("RankedNthRatioValues");

  return(($goProfileSetCurve, $goProfileSetGene));
}

1;


# pberANKA_Kent_Induce_Gameto_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_7b1bac6cd1;
use Data::Dumper;

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $colors = ['#F08080', '#7CFC00' ];
 # my $legend = ['Control', 'Induced' ];

  my $id = $self->getId();

  my $rAdjustString = << 'RADJUST';
  profile.df.full$LEGEND <- unlist(lapply(strsplit(as.character(profile.df.full$ELEMENT_NAMES), " "), "[", 1))
  profile.df.full$GROUP <- profile.df.full$LEGEND
RADJUST

  # Sense
  my @profileArray = (['P. berghei transcriptome during inducible gametocytogenesis [htseq-union - firststrand - tpm - unique]', 'values']);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $line->setPartName('tpm_sense');
  $line->setProfileSets($profileSets);
  $line->setColors($colors);
  $line->setHasExtraLegend(1);
  $line->setXaxisLabel("Hours");
  $line->setYaxisLabel("TPM");
  $line->setPlotTitle("tpm_sense - $id");
  $line->addAdjustProfile($rAdjustString);

  # AntiSense
  my @profileArray2 = (['P. berghei transcriptome during inducible gametocytogenesis [htseq-union - secondstrand - tpm - unique]', 'values']);

  my $profileSets2 = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray2);
  my $line2 = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $line2->setPartName('tpm_antisense');
  $line2->setProfileSets($profileSets2);
  $line2->setColors($colors);
  $line2->setHasExtraLegend(1);
  $line2->setXaxisLabel("Hours");
  $line2->setYaxisLabel("TPM");
  $line2->setPlotTitle("tpm_antisense - $id");
  $line2->addAdjustProfile($rAdjustString);

  #percentile_sense
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets
    ([['P. berghei transcriptome during inducible gametocytogenesis [htseq-union - firststrand - tpm - unique]', 'channel1_percentiles']]);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::Percentile->new(@_);
  $percentile->setPartName('percentile_sense');
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setHasExtraLegend(1);
  $percentile->setXaxisLabel("Hours");
  $percentile->setYaxisLabel("Percentile");
  $percentile->setPlotTitle("pct_sense - $id");
  $percentile->addAdjustProfile($rAdjustString);

  #percentile_antisense
  my $percentileSets2 = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets
    ([['P. berghei transcriptome during inducible gametocytogenesis [htseq-union - secondstrand - tpm - unique]', 'channel1_percentiles']]);

  my $percentile2 = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::Percentile->new(@_);
  $percentile2->setPartName('percentile_antisense');
  $percentile2->setProfileSets($percentileSets2);
  $percentile2->setColors($colors);
  $percentile2->setHasExtraLegend(1);
  $percentile2->setXaxisLabel("Hours");
  $percentile2->setYaxisLabel("Percentile");
  $percentile2->setPlotTitle("pct_antisense - $id");
  $percentile2->addAdjustProfile($rAdjustString);

  $self->setGraphObjects($line, $line2, $percentile, $percentile2);
  return $self;
}

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
#    print STDERR Dumper($_);
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /nonunique/){
    return 1;
  }
  return 0;
}

1;



package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_af621bdb28;

sub init {
  
  my $self = shift;
  $self->SUPER::init(@_);

#  $self->setXAxisLabel("hours");
  my @colors = ('gray','gray','#D87093','#D87093');

  # Draw the diff first in light grey ... then the min rpkm will go on top
  my @profileArray = (['Nematocida parisii ERTm1 Spores [htseq-union - unstranded - fpkm - unique]', 'values', ''],
                      ['C. elegans Time Series - Infected [htseq-union - unstranded - fpkm - unique]', 'values', ''],
                      ['Nematocida parisii ERTm1 Spores [htseq-union - unstranded - fpkm - nonunique]', 'values', ''],
                      ['C. elegans Time Series - Infected [htseq-union - unstranded - fpkm - nonunique]', 'values', ''],
                     );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets
 ([['Nematocida parisii ERTm1 Spores [htseq-union - unstranded - fpkm - unique]', 'channel1_percentiles', ''],
  ['C. elegans Time Series - Infected [htseq-union - unstranded - fpkm - unique]', 'channel1_percentiles', '']]
                     );

#  my $additionalRCode = "lines.df[2,] = lines.df[2,] + lines.df[3,];";


  my $stacked = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::PairedEndRNASeq->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);

#  $stacked->addAdjustProfile($additionalRCode);
  $stacked->setXaxisLabel("hours");
  $stacked->setPointsPch([19,'NA','NA']);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[2]]);
  $percentile->setForceHorizontalXAxis(1);

  my $legend = ['Spores', '8 hrs', '16 hrs', '30 hrs', '40 hrs', '64 hrs'];
  $percentile->setSampleLabels($legend);

  $stacked->setElementNameMarginSize(6);
  $percentile->setElementNameMarginSize(6);


  $self->setGraphObjects($stacked, $percentile);


  return $self;
}

1;


# pfal3D7_Stunnenberg_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_a239464cb4;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24];
  my $colors = ['#E9967A', '#4682B4', '#DDDDDD'];

  my @profileArray = (['pfal3D7_Stunnenberg_pi_time_series [htseq-union - unstranded - tpm - unique]', 'values'],
                      ['pfal3D7_Stunnenberg_pi_time_series - scaled [htseq-union - unstranded - tpm - unique]', 'values'],
                     );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $line->setProfileSets($profileSets);
  $line->setPartName('tpm_line');
  $line->setAdjustProfile('profile.df.full$VALUE = log2(profile.df.full$VALUE + 1);');
  $line->setYaxisLabel('log2(TPM + 1)');
  $line->setPointsPch($pch);
  $line->setXaxisLabel("Timepoint");
  $line->setColors([$colors->[0], $colors->[1]]);
  $line->setHasExtraLegend(1);
  $line->setLegendLabels(['Normal', 'Scaled']);

  my $id = $self->getId();
  $line->setPlotTitle("TPM - $id");

  my $graphObjects = $self->getGraphObjects();
  my $barProfile = $graphObjects->[0];
  $barProfile->setPartName('tpm');

  my $scaledProfile = $graphObjects->[1];
  $scaledProfile->setColors([$colors->[1]]);

  unshift (@$graphObjects, $line);
  $self->setGraphObjects(@$graphObjects);

  return $self;
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_173528b522;
use Data::Dumper;
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['blue', 'gray'];

  #tpm sense combined
  my @profileArray = (['Transcriptomes of enteroepithelial stages [htseq-union - firststrand - nonunique.tpm - nonunique]', 'values'],
                      ['Transcriptomes of enteroepithelial stages [htseq-union - firststrand - nonunique.tpm - nonunique]', 'standard_error'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - firststrand - nonunique.tpm - nonunique]', 'values'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - firststrand - nonunique.tpm - nonunique]', 'standard_error'],
                      ['Transcriptomes of enteroepithelial stages [htseq-union - firststrand - tpm - unique]', 'values'],
                      ['Transcriptomes of enteroepithelial stages [htseq-union - firststrand - tpm - unique]', 'standard_error'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - firststrand - tpm - unique]', 'values'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - firststrand - tpm - unique]', 'standard_error'],
                     );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $bar = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);
  $bar->setProfileSets($profileSets);
  $bar->setPartName('tpm_sense');
  $bar->setYaxisLabel('TPM');
  $bar->setColors([$colors->[1], $colors->[0]]);
  $bar->setIsStacked(1);
  $bar->setHasExtraLegend(1);
  $bar->setLegendLabels(['nonunique', 'nonunique', 'unique', 'unique']);
  my $rAdjustString = << 'RADJUST';
    profile.df.full$LEGEND <- as.factor(ifelse(grepl('nonunique', profile.df.full$PROFILE_SET, fixed=T), 'nonunique', 'unique'))
    newVals <- aggregate(VALUE ~ NAME, with(profile.df.full, data.frame(NAME=NAME, VALUE=ifelse(LEGEND=="nonunique", 1, -1)*VALUE)), sum);
    profile.df.full$VALUE[profile.df.full$LEGEND == "nonunique" & profile.df.full$NAME %in% newVals$NAME] <- newVals$VALUE;
    profile.df.full$VALUE[profile.df.full$VALUE < 0] <- 0;
    profile.df.full$STACK <- paste0(profile.df.full$NAME, '- ', profile.df.full$LEGEND, ' reads')
    profile.df.full$STDERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MAX_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MIN_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    if (any(c('unique', 'nonunique') %in% profile.df.full$LEGEND)) {
      profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels=c('nonunique', 'unique'))
    }
    profile.df.full$FACET <- ifelse(grepl('enteroepithelial', profile.df.full$PROFILE_SET, fixed=T), 'Enteroepithelial', 'Sporulation')
    profile.df.full$FACET <- factor(profile.df.full$FACET, levels=c('Sporulation','Enteroepithelial'))

RADJUST
  $bar->addAdjustProfile($rAdjustString);
  $bar->setRPostscript("gp = gp + facet_grid('. ~ FACET', scales='free_x', space='free_x')");
  my $id = $self->getId();
  $bar->setPlotTitle("tpm_sense - $id");

  # tpm antisense combined
  my @profileArrayAntisense = (['Transcriptomes of enteroepithelial stages [htseq-union - secondstrand - nonunique.tpm - nonunique]', 'values'],
                      ['Transcriptomes of enteroepithelial stages [htseq-union - secondstrand - nonunique.tpm - nonunique]', 'standard_error'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - secondstrand - nonunique.tpm - nonunique]', 'values'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - secondstrand - nonunique.tpm - nonunique]', 'standard_error'],
                      ['Transcriptomes of enteroepithelial stages [htseq-union - secondstrand - tpm - unique]', 'values'],
                      ['Transcriptomes of enteroepithelial stages [htseq-union - secondstrand - tpm - unique]', 'standard_error'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - secondstrand - tpm - unique]', 'values'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - secondstrand - tpm - unique]', 'standard_error'],
                     );

  my $profileSetsAntisense = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayAntisense);

  my $barAntisense = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);
  $barAntisense->setProfileSets($profileSetsAntisense);
  $barAntisense->setPartName('tpm_antisense');
  $barAntisense->setYaxisLabel('TPM');
  $barAntisense->setColors([$colors->[1], $colors->[0]]);
  $barAntisense->setHasExtraLegend(1);
  $barAntisense->setLegendLabels(['nonunique', 'nonunique', 'unique', 'unique']);
  $barAntisense->setIsStacked(1);
  $barAntisense->addAdjustProfile($rAdjustString);
  $barAntisense->setRPostscript("gp = gp + facet_grid('. ~ FACET', scales='free_x', space='free_x')");
  $barAntisense->setPlotTitle("tpm_antisense - $id");

 #tpm both strands
  my @profileArrayBothStrands = (['Transcriptomes of enteroepithelial stages [htseq-union - firststrand - tpm - unique]', 'values', 'Transcriptomes of enteroepithelial stages [htseq-union - firststrand - tpm - unique]', 'standard_error'],
                         ['Unsporulated and sporulated T. gondii [htseq-union - firststrand - tpm - unique]', 'values', 'Unsporulated and sporulated T. gondii [htseq-union - firststrand - tpm - unique]', 'standard_error'],
                         ['Transcriptomes of enteroepithelial stages [htseq-union - secondstrand - tpm - unique]', 'values', 'Transcriptomes of enteroepithelial stages [htseq-union - secondstrand - tpm - unique]', 'standard_error'],
                         ['Unsporulated and sporulated T. gondii [htseq-union - secondstrand - tpm - unique]', 'values', 'Unsporulated and sporulated T. gondii [htseq-union - secondstrand - tpm - unique]', 'standard_error'],
                        );

  my $profileSetsBothStrands = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayBothStrands);
  my @args = (@_, $profileSetsBothStrands);

  my $barBothStrands = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::RNASeqSenseAntisense->new(@args);
  $barBothStrands->setProfileSets($profileSetsBothStrands);
  $barBothStrands->setPartName('tpm_Both_strands');
  $barBothStrands->setYaxisLabel('log2(TPM + 1)');
  $barBothStrands->setColors([$colors->[0], $colors->[1]]);
  $barBothStrands->setIsStacked(0);
  my $rAdjustStringSenseAntisense = << 'RADJUST';
    profile.df.full$FACET <- ifelse(grepl('enteroepithelial', profile.df.full$PROFILE_SET, fixed=T), 'Enteroepithelial', 'Sporulation')
    profile.df.full$FACET <- factor(profile.df.full$FACET, levels=c('Sporulation','Enteroepithelial'))
RADJUST
  $barBothStrands->setWantLogged(1);
  $barBothStrands->addAdjustProfile($rAdjustStringSenseAntisense);
  $barBothStrands->setRPostscript("gp = gp + facet_grid('. ~ FACET', scales='free_x', space='free_x')");
  $barBothStrands->setPlotTitle("tpm_Both_strands - $id");


  # percentile sense combined
  my @profileArrayPercentile = (['Transcriptomes of enteroepithelial stages [htseq-union - firststrand - tpm - unique]', 'channel1_percentiles'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - firststrand - tpm - unique]', 'channel1_percentiles'],
                     );

  my $profileSetsPercentile = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayPercentile);

  my $barPercentile = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);
  $barPercentile->setProfileSets($profileSetsPercentile);
  $barPercentile->setPartName('percentile_sense');
  $barPercentile->setYaxisLabel('Percentile');
  $barPercentile->setColors([$colors->[0]]);
  $barPercentile->addAdjustProfile($rAdjustString);
  $barPercentile->setRPostscript("gp = gp + facet_grid('. ~ FACET', scales='free_x', space='free_x')");
  $barPercentile->setPlotTitle("percentile_sense - $id");
  $barPercentile->setDefaultYMax(100);

  # percentile antisense combined
  my @profileArrayPercentileAntisense = (['Transcriptomes of enteroepithelial stages [htseq-union - secondstrand - tpm - unique]', 'channel1_percentiles'],
                      ['Unsporulated and sporulated T. gondii [htseq-union - secondstrand - tpm - unique]', 'channel1_percentiles'],
                     );

  my $profileSetsPercentileAntisense = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayPercentileAntisense);

  my $barPercentileAntisense = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);
  $barPercentileAntisense->setProfileSets($profileSetsPercentileAntisense);
  $barPercentileAntisense->setPartName('percentile_antisense');
  $barPercentileAntisense->setYaxisLabel('Percentile');
  $barPercentileAntisense->setColors([$colors->[0]]);
  $barPercentileAntisense->addAdjustProfile($rAdjustString);
  $barPercentileAntisense->setRPostscript("gp = gp + facet_grid('. ~ FACET', scales='free_x', space='free_x')");
  $barPercentileAntisense->setPlotTitle("percentile_antisense - $id");
  $barPercentileAntisense->setDefaultYMax(100);  

  $self->setGraphObjects($bar, $barAntisense, $barBothStrands, $barPercentile, $barPercentileAntisense);

  return $self;
}
1;

# pfal3D7_Bartfai_time_series_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_715bf2deda;



sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $graphObjects = $self->getGraphObjects();



  my $scaledSenseProfile = $graphObjects->[3];
  $scaledSenseProfile->setColors(['#8F006B']);

  my $scaledAntiSenseProfile = $graphObjects->[4];
  $scaledAntiSenseProfile->setColors(['#8F006B']);

  return $self;

}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_d2ff7c7826;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
use strict;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setSmoothLines(0);
  $profile->setXaxisLabel("Hour");
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_f595797fd2;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
use strict;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setSmoothLines(0);
  $profile->setXaxisLabel("Hour");
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_d9ee583dcc;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
use strict;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setSmoothLines(0);
  $profile->setXaxisLabel("Hour");
}

1;


# PlasmoDB pchachabaudi_pchachabaudi_SR10KOvWT_Subudhi_RSRC_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_ab772b084e;

# @Override
sub getProfileColors {
  my ($self) = @_;
  my @colors =  @{$self->getColors()};
  return \@colors;
}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  profile.df.full <- profile.df.full[profile.df.full$LEGEND == 'unique',]
  profile.df.full$LEGEND <- as.character(profile.df.full$LEGEND)
  profile.df.full$LEGEND[grepl('WT', profile.df.full$ELEMENT_NAMES)] <- 'Wild Type'
  profile.df.full$LEGEND[profile.df.full$LEGEND != 'Wild Type'] <- 'SR10 KO'
  profile.df.full$ELEMENT_NAMES <- gsub('10KO', '', profile.df.full$ELEMENT_NAMES)
  profile.df.full$ELEMENT_NAMES_NUMERIC = as.numeric(gsub(" *[a-z-A-Z()+-]+ *", "", profile.df.full$ELEMENT_NAMES, perl=T))
  profile.df.full$GROUP <- profile.df.full$LEGEND
RADJUST

  $profile->setSmoothLines(0);
  $profile->setXaxisLabel("Hours");
  $profile->addAdjustProfile($rAdjustString);
}

1;

#plasmo - pfal3D7_Tonkin_Hill_Malaria_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_aae60e6bf7;
sub init {
  my $self = shift;
  $self->SUPER::init(@_);
  $self->setPlotWidth(1000);
  return $self;
}


#plasmo - pfal3D7_ppfal3d7_kucharski_RBC_RNAseq_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_29940f3f82;
sub init {
  my $self = shift;
  $self->SUPER::init(@_);
  $self->setPlotWidth(600);
  return $self;
}


# fungi - mory70-15_Ruiz_Mory_2020_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_9d559d0fae;

sub getPlotWidth { return 1000; }

1;



# fungi - calbSC5314_Liu_calbSC5314_2015_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_d133a8e1d9;

sub getPlotWidth { return 1200; }

1;


# FungiDB ncraOR74A_Ncrassa_mono_disaccharide_Wu_2021_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_d5cb6ab9a2;

sub getPlotWidth { return 1200; }

1;


# host - hsapREF_Liu_calbSC5314_2015_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_5f2c5bb6f7;

sub getPlotWidth { return 550; }

1;

# host - hsapREF_Dewi_Roryzae_2021_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_abeab0974c;

sub getPlotWidth { return 750; }

1;


# vect - aaegLVP_AGWG_SRP034735_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_f279b1d0f8;

sub getPlotWidth { return 750; }

1;


# vect - agamPEST_ERP113663_ebi_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_df981c3f82;

sub getPlotWidth { return 1000; }

1;



#--------------------------------------------------------------------------------
# TEMPLATE_ANCHOR rnaSeqGraph

# package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_66f9e70b8a;
# use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
# use strict;
# sub getGraphType { 'bar' }
# sub excludedProfileSetsString { '' }
# sub getSampleLabelsString {''}
# sub getColorsString {}
# sub getForceXLabelsHorizontalString { '1' } 
# sub getBottomMarginSize {  }
# sub getExprPlotPartModuleString { 'RNASeq' }
# sub getXAxisLabel { '' }

1; 

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_b1ac1e329c;

sub getPlotWidth { return 800; }

1;

