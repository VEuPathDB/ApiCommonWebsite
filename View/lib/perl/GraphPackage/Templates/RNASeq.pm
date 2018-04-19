package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;

# @Override
sub getKey{
  my ($self, $profileSetName, $profileType) = @_;
#print STDERR Dumper($profileSetName);
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
  return "${groupName}_${profileType}${strand}";
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


sub getStrandDictionary {
  my $self = shift;
  my $firststrand = 'sense';
  my $secondstrand = 'antisense';

  if ($self->switchStrands()) {
    $firststrand = 'antisense';
    $secondstrand = 'sense';
  }
  return { 'unstranded' => '',
	   'firststrand' => $firststrand,
	   'secondstrand' => $secondstrand
	 };
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

  my $rAdjustString = << 'RADJUST';
  if ('NAME' %in% colnames(profile.df.full) & 'LEGEND' %in% colnames(profile.df.full)) {
    newVals <- aggregate(VALUE ~ NAME, with(profile.df.full, data.frame(NAME=NAME, VALUE=ifelse(LEGEND=="nonunique", 1, -1)*VALUE)), sum);
    profile.df.full$VALUE[profile.df.full$LEGEND == "nonunique" & profile.df.full$NAME == newVals$NAME] <- newVals$VALUE;
    profile.df.full$VALUE[profile.df.full$VALUE < 0] <- 0;
    profile.df.full$STACK <- paste0(profile.df.full$NAME, "- ", profile.df.full$LEGEND, " reads");
    #profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels = rev(levels(as.factor(profile.df.full$LEGEND))));
    profile.df.full$STDERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MAX_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MIN_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
  }
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

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_4b0e1b490a;

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
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_4585d065bf;

sub init {
  my $self = shift;
  use Data::Dumper; 

  $self->SUPER::init(@_);

  #bit hackish but it was a quick solution and i dont suspect ill have to do this more than this once.
  my @elementNames_RMe14 = ['Day 0', 'Day 1', 'Day 100', 'Day 2', 'Day 23', 'Day 24', 'Day 27', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 53', 'Day 54', 'Day 55', 'Day 56', 'Day 57', 'Day 58', 'Day 59', 'Day 6', 'Day 60', 'Day 61', 'Day 62', 'Day 63', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 9', 'Day 90', 'Day 93', 'Day 94', 'Day 95', 'Day 96', 'Day 97', 'Day 98', 'Day 99', 'Day 91', 'Day 11', 'Day 82', 'Day 17', 'Day 85', 'Day 80', 'Day 83', 'Day 22', 'Day 15', 'Day 18', 'Day 88', 'Day 92', 'Day 20', 'Day 26', 'Day 12', 'Day 19', 'Day 13', 'Day 21', 'Day 25', 'Day 10', 'Day 87', 'Day 89', 'Day 16', 'Day 14', 'Day 86', 'Day 84', 'Day 81'];
  my @elementNames_RFv13 = ['Day 0', 'Day 1', 'Day 10', 'Day 11', 'Day 2', 'Day 3', 'Day 4', 'Day 5', 'Day 6', 'Day 7', 'Day 8', 'Day 9', 'Day 19', 'Day 12', 'Day 22', 'Day 13', 'Day 16', 'Day 14', 'Day 15', 'Day 17', 'Day 21', 'Day 20', 'Day 18'];
  my @elementNames_RIc14 = ['Day 0', 'Day 1', 'Day 10', 'Day 100', 'Day 2', 'Day 27', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 6', 'Day 61', 'Day 62', 'Day 63', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 80', 'Day 81', 'Day 82', 'Day 89', 'Day 9', 'Day 90', 'Day 91', 'Day 92', 'Day 93', 'Day 94', 'Day 95', 'Day 96', 'Day 97', 'Day 98', 'Day 99', 'Day 54', 'Day 83', 'Day 24', 'Day 25', 'Day 23', 'Day 87', 'Day 26', 'Day 20', 'Day 88', 'Day 17', 'Day 16', 'Day 84', 'Day 55', 'Day 60', 'Day 12', 'Day 19', 'Day 53', 'Day 11', 'Day 13', 'Day 18', 'Day 58', 'Day 21', 'Day 59', 'Day 15', 'Day 57', 'Day 85', 'Day 86', 'Day 14', 'Day 22', 'Day 56'];
  my @elementNames_RSb14 = ['Day 0', 'Day 1', 'Day 10', 'Day 100', 'Day 2', 'Day 27', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 53', 'Day 54', 'Day 55', 'Day 56', 'Day 57', 'Day 58', 'Day 59', 'Day 6', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 80', 'Day 81', 'Day 82', 'Day 83', 'Day 84', 'Day 85', 'Day 86', 'Day 87', 'Day 9', 'Day 94', 'Day 95', 'Day 96', 'Day 97', 'Day 98', 'Day 99', 'Day 16', 'Day 18', 'Day 25', 'Day 63', 'Day 60', 'Day 89', 'Day 61', 'Day 20', 'Day 62', 'Day 12', 'Day 24', 'Day 19', 'Day 93', 'Day 92', 'Day 88', 'Day 11', 'Day 23', 'Day 14', 'Day 15', 'Day 13', 'Day 90', 'Day 91', 'Day 26', 'Day 21', 'Day 17', 'Day 22'];
  my @elementNames_RFa14 = ['Day 0', 'Day 1', 'Day 10', 'Day 11', 'Day 2', 'Day 21', 'Day 22', 'Day 23', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 53', 'Day 54', 'Day 55', 'Day 56', 'Day 57', 'Day 58', 'Day 59', 'Day 6', 'Day 60', 'Day 61', 'Day 62', 'Day 63', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 80', 'Day 81', 'Day 82', 'Day 83', 'Day 84', 'Day 85', 'Day 86', 'Day 87', 'Day 88', 'Day 89', 'Day 9', 'Day 90', 'Day 91', 'Day 92', 'Day 93', 'Day 94', 'Day 19', 'Day 99', 'Day 27', 'Day 24', 'Day 16', 'Day 97', 'Day 96', 'Day 12', 'Day 98', 'Day 17', 'Day 18', 'Day 15', 'Day 95', 'Day 13', 'Day 14', 'Day 26', 'Day 20', 'Day 25', 'Day 100'];

  my @profileArray = (['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RMe14, '', '', '', '', '', '', 'RMe14'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RFv13, '', '', '', '', '', '', 'RFv13'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RIc14, '', '', '', '', '', '', 'RIc14'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RSb14, '', '', '', '', '', '', 'RSb14'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RFa14, '', '', '', '', '', '', 'RFa14'],  
                   );

  my $profileSet = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  #print STDERR Dumper($line);
  $line->setPartName('parasitemia');
  $line->setProfileSets($profileSet);
  $line->setYaxisLabel("Log 10 Parasites / uL");
  $line->setXaxisLabel("Day");
  $line->setPlotTitle("Parasitemia Summary - 100 Days");
  
  my $rAdjustString = << 'RADJUST';

  profile.df.full$VALUE <- log10(profile.df.full$VALUE)
  profile.df.full$VALUE[is.infinite(profile.df.full$VALUE)] <- 0
  profile.df.full <- separate(profile.df.full, PROFILE_FILE, c("trash", "FACET", "trash2"), "-")
  profile.df.full$FACET <- as.factor(profile.df.full$FACET) 
  profile.df.full$PROFILE_FILE <- profile.df.full$FACET
  profile.df.full$trash <- NULL
  profile.df.full$trash2 <- NULL
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
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_40a06f276b;

sub init {
  my $self = shift;
  use Data::Dumper; 

  $self->SUPER::init(@_);

  #bit hackish but it was a quick solution and i dont suspect ill have to do this more than this once.
  my @elementNames_RMe14 = ['Day 0', 'Day 1', 'Day 100', 'Day 2', 'Day 23', 'Day 24', 'Day 27', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 53', 'Day 54', 'Day 55', 'Day 56', 'Day 57', 'Day 58', 'Day 59', 'Day 6', 'Day 60', 'Day 61', 'Day 62', 'Day 63', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 9', 'Day 90', 'Day 93', 'Day 94', 'Day 95', 'Day 96', 'Day 97', 'Day 98', 'Day 99', 'Day 91', 'Day 11', 'Day 82', 'Day 17', 'Day 85', 'Day 80', 'Day 83', 'Day 22', 'Day 15', 'Day 18', 'Day 88', 'Day 92', 'Day 20', 'Day 26', 'Day 12', 'Day 19', 'Day 13', 'Day 21', 'Day 25', 'Day 10', 'Day 87', 'Day 89', 'Day 16', 'Day 14', 'Day 86', 'Day 84', 'Day 81'];
  my @elementNames_RFv13 = ['Day 0', 'Day 1', 'Day 10', 'Day 11', 'Day 2', 'Day 3', 'Day 4', 'Day 5', 'Day 6', 'Day 7', 'Day 8', 'Day 9', 'Day 19', 'Day 12', 'Day 22', 'Day 13', 'Day 16', 'Day 14', 'Day 15', 'Day 17', 'Day 21', 'Day 20', 'Day 18'];
  my @elementNames_RIc14 = ['Day 0', 'Day 1', 'Day 10', 'Day 100', 'Day 2', 'Day 27', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 6', 'Day 61', 'Day 62', 'Day 63', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 80', 'Day 81', 'Day 82', 'Day 89', 'Day 9', 'Day 90', 'Day 91', 'Day 92', 'Day 93', 'Day 94', 'Day 95', 'Day 96', 'Day 97', 'Day 98', 'Day 99', 'Day 54', 'Day 83', 'Day 24', 'Day 25', 'Day 23', 'Day 87', 'Day 26', 'Day 20', 'Day 88', 'Day 17', 'Day 16', 'Day 84', 'Day 55', 'Day 60', 'Day 12', 'Day 19', 'Day 53', 'Day 11', 'Day 13', 'Day 18', 'Day 58', 'Day 21', 'Day 59', 'Day 15', 'Day 57', 'Day 85', 'Day 86', 'Day 14', 'Day 22', 'Day 56'];
  my @elementNames_RSb14 = ['Day 0', 'Day 1', 'Day 10', 'Day 100', 'Day 2', 'Day 27', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 53', 'Day 54', 'Day 55', 'Day 56', 'Day 57', 'Day 58', 'Day 59', 'Day 6', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 80', 'Day 81', 'Day 82', 'Day 83', 'Day 84', 'Day 85', 'Day 86', 'Day 87', 'Day 9', 'Day 94', 'Day 95', 'Day 96', 'Day 97', 'Day 98', 'Day 99', 'Day 16', 'Day 18', 'Day 25', 'Day 63', 'Day 60', 'Day 89', 'Day 61', 'Day 20', 'Day 62', 'Day 12', 'Day 24', 'Day 19', 'Day 93', 'Day 92', 'Day 88', 'Day 11', 'Day 23', 'Day 14', 'Day 15', 'Day 13', 'Day 90', 'Day 91', 'Day 26', 'Day 21', 'Day 17', 'Day 22'];
  my @elementNames_RFa14 = ['Day 0', 'Day 1', 'Day 10', 'Day 11', 'Day 2', 'Day 21', 'Day 22', 'Day 23', 'Day 28', 'Day 29', 'Day 3', 'Day 30', 'Day 31', 'Day 32', 'Day 33', 'Day 34', 'Day 35', 'Day 36', 'Day 37', 'Day 38', 'Day 39', 'Day 4', 'Day 40', 'Day 41', 'Day 42', 'Day 43', 'Day 44', 'Day 45', 'Day 46', 'Day 47', 'Day 48', 'Day 49', 'Day 5', 'Day 50', 'Day 51', 'Day 52', 'Day 53', 'Day 54', 'Day 55', 'Day 56', 'Day 57', 'Day 58', 'Day 59', 'Day 6', 'Day 60', 'Day 61', 'Day 62', 'Day 63', 'Day 64', 'Day 65', 'Day 66', 'Day 67', 'Day 68', 'Day 69', 'Day 7', 'Day 70', 'Day 71', 'Day 72', 'Day 73', 'Day 74', 'Day 75', 'Day 76', 'Day 77', 'Day 78', 'Day 79', 'Day 8', 'Day 80', 'Day 81', 'Day 82', 'Day 83', 'Day 84', 'Day 85', 'Day 86', 'Day 87', 'Day 88', 'Day 89', 'Day 9', 'Day 90', 'Day 91', 'Day 92', 'Day 93', 'Day 94', 'Day 19', 'Day 99', 'Day 27', 'Day 24', 'Day 16', 'Day 97', 'Day 96', 'Day 12', 'Day 98', 'Day 17', 'Day 18', 'Day 15', 'Day 95', 'Day 13', 'Day 14', 'Day 26', 'Day 20', 'Day 25', 'Day 100'];

  my @profileArray = (['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RMe14, '', '', '', '', '', '', 'RMe14'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RFv13, '', '', '', '', '', '', 'RFv13'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RIc14, '', '', '', '', '', '', 'RIc14'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RSb14, '', '', '', '', '', '', 'RSb14'],
                      ['Parasitemia over 100 days in five monkeys', 'values', '', '', @elementNames_RFa14, '', '', '', '', '', '', 'RFa14'],  
                   );

  my $profileSet = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  #print STDERR Dumper($line);
  $line->setPartName('parasitemia');
  $line->setProfileSets($profileSet);
  $line->setYaxisLabel("Log 10 Parasites / uL");
  $line->setXaxisLabel("Day");
  $line->setPlotTitle("Parasitemia Summary - 100 Days");
  
  my $rAdjustString = << 'RADJUST';

  profile.df.full$VALUE <- log10(profile.df.full$VALUE)
  profile.df.full$VALUE[is.infinite(profile.df.full$VALUE)] <- 0
  profile.df.full <- separate(profile.df.full, PROFILE_FILE, c("trash", "FACET", "trash2"), "-")
  profile.df.full$FACET <- as.factor(profile.df.full$FACET) 
  profile.df.full$PROFILE_FILE <- profile.df.full$FACET
  profile.df.full$trash <- NULL
  profile.df.full$trash2 <- NULL
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

# TriTryp - tbruTREU927_Rijo_Circadian_Regulation_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_1cc46f3acb;

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
    profile.df.full$ELEMENT_NAMES = factor(profile.df.full$ELEMENT_NAMES,   levels=c('BSF 0hr Alt Temp', 'BSF 4hr Alt Temp', 'BSF 8hr Alt Temp', 'BSF 12hr Alt Temp', 'BSF 16hr Alt Temp', 'BSF 20hr Alt Temp', 'BSF 24hr Alt Temp', 'BSF 28hr Alt Temp', 'BSF 32hr Alt Temp', 'BSF 36hr Alt Temp', 'BSF 40hr Alt Temp', 'BSF 44hr Alt Temp', 'BSF 48hr Alt Temp',   
   'BSF 0hr Const Temp', 'BSF 4hr Const Temp', 'BSF 8hr Const Temp', 'BSF 12hr Const Temp', 'BSF 16hr Const Temp', 'BSF 20hr Const Temp', 'BSF 24hr Const Temp', 'BSF 28hr Const Temp', 'BSF 32hr Const Temp', 'BSF 36hr Const Temp', 'BSF 40hr Const Temp', 'BSF 44hr Const Temp', 'BSF 48hr Const Temp',
   'PF 0hr Alt Temp', 'PF 4hr Alt Temp', 'PF 8hr Alt Temp', 'PF 12hr Alt Temp', 'PF 16hr Alt Temp', 'PF 20hr Alt Temp', 'PF 24hr Alt Temp', 'PF 28hr Alt Temp', 'PF 32hr Alt Temp', 'PF 36hr Alt Temp', 'PF 40hr Alt Temp', 'PF 44hr Alt Temp', 'PF 48hr Alt Temp',
   'PF 0hr Const Temp', 'PF 4hr Const Temp', 'PF 8hr Const Temp', 'PF 12hr Const Temp', 'PF 16hr Const Temp', 'PF 20hr Const Temp', 'PF 24hr Const Temp', 'PF 28hr Const Temp', 'PF 32hr Const Temp', 'PF 36hr Const Temp', 'PF 40hr Const Temp', 'PF 44hr Const Temp', 'PF 48hr Const Temp' ));

   profile.df.full$GROUP = c( 
    "BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp","BSF Alt Temp",
    "BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp","BSF Const Temp",
    "PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp","PF Alt Temp",
    "PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp","PF Const Temp"
  );

  profile.df.full$PROFILE_FILE = profile.df.full$GROUP
  profile.df.full$LEGEND = profile.df.full$GROUP
RADJUST

  my $legend = ['BSF Alt Temp', 'BSF Const Temp', 'PF Alt Temp', 'BPF Const Temp'];
  $profile->setSmoothLines(0);

  $profile->setXaxisLabel('Hours');
  $profile->setLegendLabels($legend);
  $profile->addAdjustProfile($rAdjustString);
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
    profile.df.full$VALUE[profile.df.full$LEGEND == "nonunique" & profile.df.full$NAME == newVals$NAME] <- newVals$VALUE;
    profile.df.full$VALUE[profile.df.full$VALUE < 0] <- 0;
    profile.df.full$STACK <- paste0(profile.df.full$NAME, "- ", profile.df.full$LEGEND, " reads");
    #profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels = rev(levels(as.factor(profile.df.full$LEGEND))));
    profile.df.full$STDERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MAX_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MIN_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
  }
RADJUST

  $profile->addAdjustProfile($rAdjustString);

}
1;

# tbruTREU927_RNAi_Horn_*rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_3f5188c7a8;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeq );
use strict;
sub getGraphType { 'bar' }
sub excludedProfileSetsString { '' }
sub getSampleLabelsString { '' }
sub getColorsString { '#800080;#008000'  } 
sub getForceXLabelsHorizontalString { 'true' } 
sub getBottomMarginSize { 0 }
sub getExprPlotPartModuleString { 'RNASeq' }
sub getXAxisLabel { '' }
sub switchStrands {
   return 0;
}

sub getRemainderRegex {
  return qr/Horn(.*) \[/;
}

sub getProfileColors {
  my ($self) = @_;

  my @colors =  @{$self->getColors()};
  return \@colors;
}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my @labels = map {"fpkm" . $_} @{$profile->getLegendLabels()};
  $profile->setLegendLabels(\@labels);
  $profile->setIsStacked(0);

  my $rAdjustString = << 'RADJUST';
  if ('NAME' %in% colnames(profile.df.full) & 'LEGEND' %in% colnames(profile.df.full)) {
    newVals <- aggregate(VALUE ~ NAME, with(profile.df.full, data.frame(NAME=NAME, VALUE=ifelse(LEGEND=="nonunique", 1, -1)*VALUE)), sum);
    profile.df.full$VALUE[profile.df.full$LEGEND == "nonunique" & profile.df.full$NAME == newVals$NAME] <- newVals$VALUE;
    profile.df.full$VALUE[profile.df.full$VALUE < 0] <- 0;
    profile.df.full$STACK <- paste0(profile.df.full$NAME, "- ", profile.df.full$LEGEND, " reads");
    #profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels = rev(levels(as.factor(profile.df.full$LEGEND))));
    profile.df.full$STDERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MAX_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
    profile.df.full$MIN_ERR[profile.df.full$LEGEND == 'nonunique'] <- NA
  }
RADJUST
  
  $profile->addAdjustProfile($rAdjustString);
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
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_d57671ced8;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24];
  my $colors = ['#E9967A', '#4682B4', '#DDDDDD'];

  my @profileArray = (['pfal3D7_Stunnenberg_pi_time_series [htseq-union - unstranded - fpkm - unique]', 'values'],
                      ['pfal3D7_Stunnenberg_pi_time_series - scaled [htseq-union - unstranded - fpkm - unique]', 'values'],
                     );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $line->setProfileSets($profileSets);
  $line->setPartName('fpkm_line');
  $line->setAdjustProfile('profile.df.full$VALUE = log2(profile.df.full$VALUE + 1);');
  $line->setYaxisLabel('FPKM (log2)');
  $line->setPointsPch($pch);
  $line->setXaxisLabel("Timepoint");
  $line->setColors([$colors->[0], $colors->[1]]);
  $line->setHasExtraLegend(1);
  $line->setLegendLabels(['Normal', 'Scaled']);

  my $id = $self->getId();
  $line->setPlotTitle("FPKM - $id");

  my $graphObjects = $self->getGraphObjects();
  my $barProfile = $graphObjects->[0];
  $barProfile->setPartName('fpkm');

  my $scaledProfile = $graphObjects->[1];
  $scaledProfile->setColors([$colors->[1]]);

  unshift (@$graphObjects, $line);
  $self->setGraphObjects(@$graphObjects);

  return $self;
}
1;

# pfal3D7_Bartfai_time_series_rnaSeq_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_715bf2deda;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $graphObjects = $self->getGraphObjects();

  my $scaledSenseProfile = $graphObjects->[2];
  $scaledSenseProfile->setColors(['#8F006B']);

  my $scaledAntiSenseProfile = $graphObjects->[3];
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
