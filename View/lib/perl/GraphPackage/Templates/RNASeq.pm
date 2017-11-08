package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;

# @Override
sub getKey{
  my ($self, $profileSetName, $profileType) = @_;

  my ($groupName) = $self->getGroupNameFromProfileSetName($profileSetName);

  my ($strand) = $profileSetName =~ /\[.+ \- (.+) \- .+ \- /;
  $groupName = '' if (!$groupName);
  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');

  die if (!$strand);
  $strand = $strand eq 'unstranded'? ''  :  '_' . $self->getStrandDictionary()->{$strand};
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
  newVals <- aggregate(VALUE ~ NAME, with(profile.df.full, data.frame(NAME=NAME, VALUE=ifelse(LEGEND=="nonunique", 1, -1)*VALUE)), sum);
  profile.df.full$VALUE[profile.df.full$LEGEND == "nonunique" & profile.df.full$NAME == newVals$NAME] <- newVals$VALUE;
  profile.df.full$VALUE[profile.df.full$VALUE < 0] <- 0;
  profile.df.full$STACK <- paste0(profile.df.full$NAME, "- ", profile.df.full$LEGEND, " reads");
  profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels = rev(levels(as.factor(profile.df.full$LEGEND))));
RADJUST

  $profile->addAdjustProfile($rAdjustString);

}

1;



package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_40a06f276b;

sub finalProfileAdjustments {                                                                                
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';

  namesSplit = strsplit(as.character(profile.df.full$NAME), " ");

  profile.df.full$LEGEND = as.factor(unlist(lapply(namesSplit, "[", 3)));
  profile.df.full$SAMPLE_TYPE = as.factor(unlist(lapply(lapply(namesSplit, "[", 1:2), paste, collapse=" ")));

  newNames = unlist(lapply(lapply(namesSplit, "[", -c(1,2,3)), paste, collapse=" "));
  newNames = gsub(" infection", " ", gsub(" of ", " ", newNames, ignore.case=T), ignore.case=T)
  profile.df.full$NAME = factor(newNames, levels=unique(newNames));

  hideLegend=FALSE;
  expandColors=FALSE;

RADJUST

  $profile->addAdjustProfile($rAdjustString);

  $profile->setFacets(["SAMPLE_TYPE"]);
  $profile->forceAutoColors(1);
#  $profile->setColors(["red","purple", "green","blue", "yellow"]);
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

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /Circadian Control of Bloodstream and Procyclic Form Transcriptomes - /){
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
}
1;


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

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my @labels = map {"fpkm" . $_} @{$profile->getLegendLabels()};
  $profile->setLegendLabels(\@labels);
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_af621bdb28;

sub init {
  
  my $self = shift;
  $self->SUPER::init(@_);

#  $self->setXAxisLabel("hours");
  my @colors = ('#D87093','#D87093');

  # Draw the diff first in light grey ... then the min rpkm will go on top
  my @profileArray = (['Nematocida parisii ERTm1 Spores [htseq-union - unstranded - fpkm]', 'values', ''],
                      ['C. elegans Time Series - Infected [htseq-union - unstranded - fpkm]', 'values', ''],
                     );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets
 ([['Nematocida parisii ERTm1 Spores [htseq-union - unstranded - fpkm]', 'channel1_percentiles', ''],
  ['C. elegans Time Series - Infected [htseq-union - unstranded - fpkm]', 'channel1_percentiles', '']]
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
  $percentile->setColors([$colors[0]]);
  $percentile->setForceHorizontalXAxis(1);

  my $legend = ['Spores', '8 hrs', '16 hrs', '30 hrs', '40 hrs', '64 hrs'];
  $percentile->setSampleLabels($legend);

  $stacked->setElementNameMarginSize(6);
  $percentile->setElementNameMarginSize(6);


  $self->setGraphObjects($stacked, $percentile);


  return $self;
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::RNASeq::DS_d57671ced8;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24];
  my $colors = ['#E9967A', '#4682B4', '#DDDDDD'];

  my @profileArray = (['pfal3D7_Stunnenberg_pi_time_series [htseq-union - unstranded - fpkm - unique]', 'values'],
                      ['pfal3D7_Stunnenberg_pi_time_series - scaled [htseq-union - unstranded - fpkm - nonunique]', 'values'],
                     );


  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
#  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['pfal3D7_Stunnenberg_pi_time_series [htseq-union - unstranded - fpkm - unique]', 'channel1_percentiles']]);

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $line->setProfileSets($profileSets);
  $line->setPartName('fpkm');
  $line->setAdjustProfile('profile.df.full$VALUE = log2(profile.df.full$VALUE + 1);');
  $line->setYaxisLabel('FPKM (log2)');
  $line->setPointsPch($pch);
  $line->setXaxisLabel("Timepoint");
  $line->setColors([$colors->[0], $colors->[1]]);

  $line->setHasExtraLegend(1);
  $line->setLegendLabels(['Normal', 'Scaled']);

  my $id = $self->getId();
  $line->setPlotTitle("FPKM - $id");

  # my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::Percentile->new(@_);
  # $percentile->setProfileSets($percentileSets);
  # $percentile->setColors([$colors->[0]]);
  # $percentile->setXaxisLabel("Timepoint");
  # $percentile->setAdjustProfile(undef);

  my @existingGraphObjects = $self->getGraphObjects();

  $self->setGraphObjects($line, @existingGraphObjects);

  return $self;
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
