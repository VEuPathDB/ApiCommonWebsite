package ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;

use EbrcWebsiteCommon::View::GraphPackage::Util;

use EbrcWebsiteCommon::View::GraphPackage::LegacyGGBarPlot;
use EbrcWebsiteCommon::View::GraphPackage::LegacyGGLinePlot;
use EbrcWebsiteCommon::View::GraphPackage::LegacyGGScatterPlot;
use EbrcWebsiteCommon::View::GraphPackage::LegacyGGPiePlot;
use EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot;
use EbrcWebsiteCommon::View::GraphPackage::GGLinePlot;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;
use EbrcWebsiteCommon::View::GraphPackage::GGPiePlot;

use Scalar::Util qw /blessed/;
use Data::Dumper;
use LWP::Simple;
use JSON;

# Subclasses can adjust the RCode but we won't let the templates do this
sub getPercentileRAdjust {}
sub getProfileRAdjust {}

sub finalProfileAdjustments {}

# Template subclasses need to implement this....should return 'bar' or 'line'
sub getGraphType {}

#default is to not keep legend entries if there is only one value in the array
sub keepSingleLegend{0}

sub restrictProfileSetsBySourceId {}

sub getGroupNameFromProfileSetName {
  my ($self, $profileSetName) = @_;
  my $regex = $self->getGroupRegex();

  $profileSetName =~ /$regex/;
  my $rv;
  if ($1) { $rv =$1;}
  return $rv;
}

sub getRemainderNameFromProfileSetName {
  my ($self, $profileSetName) = @_;
  my $regex = $self->getRemainderRegex();

  $profileSetName =~ /$regex/;
  my $rv;
  if ($1) { $rv =$1;}
  return $rv;
}


sub getGroupRegex {
  return qr/.+/;
}

sub getRemainderRegex {
  return qr/(.+)/;
}

sub getKeys {
    my ($self, $profileSetName, $profileType) = @_;
    my $key = $self->getKey($profileSetName, $profileType);
    return [$key];
}

sub getKey {
  my ($self, $profileSetName, $profileType) = @_;

  my $groupName = $self->getGroupNameFromProfileSetName($profileSetName);
#print STDERR "groupName = $groupName FOR $profileSetName\n";
  $groupName = '' if (!$groupName);
  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');
  $profileType = 'percentile' if ($profileType eq 'channel2_percentiles');

  return "${groupName}_${profileType}";
}

sub getPercentileGraphType {
  my $self = shift;
  return $self->getGraphType();
}

sub sortPercentileProfiles {
  $a->{profileName} cmp $b->{profileName} && $a->{profileType} cmp $b->{profileType};
}

# Template subclasses need to implement this....should return a valid PlotPart for the given Graph Type (LogRatio, RMA, ...)
sub getExprPlotPartModuleString {}

# Template subclasses need to implement this....should be semicolon list of colors
sub getColorsString { }

# Template subclasses need to implement this... should be true/false
sub getForceXLabelsHorizontalString {}

# Template subclasses should override if we have loaded extra profilesets which are not to be graphed
sub excludedProfileSetsString { }

# Template subclasses should override if we want to change the sample names
sub getSampleLabelsString { [] }

# Template subclasses should override if required
sub getExprMetric {}

sub setExpressionMetric {
    my ($self, $exprMetric) = @_;
    $self->{_expr_metric} = $exprMetric;
}

sub getExpressionMetric {
    my $self = shift;   
    my $exprMetric = $self->{_expr_metric};
    return $exprMetric;
}


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $allProfileSets = $self->getAllProfileSetNames();
  # print STDERR Dumper($allProfileSets);

  my %plotParts;
  my %hasStdError;


  foreach my $p (@{$allProfileSets}){
    my $profileName = $p->{profileName};
    my $profileType = $p->{profileType};
    my $keys = $self->getKeys($profileName, $profileType);

    if ($profileType eq 'standard_error') {
      $hasStdError{$profileName} = 1;
    } else {
      foreach my $key (@$keys) {
        push @{$plotParts{$key}}, $p;
      }
    }
  }

  $self->makeAndSetPlots(\%plotParts, \%hasStdError);

  return $self;
}

sub mapSampleLabels {
  my ($self, $profile, $hash) = @_;

  my $sampleLabels = $profile->getSampleLabels();
  my @adjustedSampleLabels = map { $hash->{$_} } @$sampleLabels;
  $profile->setSampleLabels(\@adjustedSampleLabels);
}


sub getAllProfileSetNames {
  my ($self) = @_;

  my $datasetId = $self->getDatasetId();
  my $id = $self->getId();
  my $restrictProfileSetsBySourceId = $self->restrictProfileSetsBySourceId();

  my @rv = ();

  if ($self->useLegacy()) {
    my $dbh = $self->getQueryHandle();
    my $sql = EbrcWebsiteCommon::View::GraphPackage::Util::getProfileSetsSql($restrictProfileSetsBySourceId, $self->getId());
    my $sh = $dbh->prepare($sql);
    $sh->execute($datasetId);
  
    while(my ($profileName, $profileType) = $sh->fetchrow_array()) {
      next if($self->isExcludedProfileSet($profileName));
      my $p = {profileName=>$profileName, profileType=>$profileType};
      push @rv, $p;
    }
    $sh->finish();
  } else {
    my $url = $self->getBaseUrl() . '/a/service/profileSet/ProfileSetNames/' . $datasetId;
    $url = $restrictProfileSetsBySourceId ? $url . '?sourceId=' . $id : $url;
    my $content = get($url);
    my $json = from_json($content);
    foreach my $profile (@$json) {
      my $profileName = $profile->{'profile_set_name'};
      my $profileType = $profile->{'profile_type'};
      next if($self->isExcludedProfileSet($profileName));
      my $p = {profileName=>$profileName, profileType=>$profileType};
      push @rv, $p;
    }
  }

  return \@rv;
}

sub sortKeys {
  my ($self, $a_tmp, $b_tmp) = @_;
  $a_tmp  =~s/^_//;
  $b_tmp =~s/^_//;

  my ($a_type, $a_suffix) = split(/\_/, $a_tmp);
  my ($b_type, $b_suffix) = split(/\_/, $b_tmp);

  return ($b_type cmp $a_type) || ($a_suffix cmp $b_suffix);

}

sub orderPlotProfiles {
  my ($self, $plotProfiles) = @_;
  my @sortedPlotProfiles = sort {$a->{profileName}.$a->{profileType} cmp $b->{profileName}.$b->{profileType}} @$plotProfiles;
  return \@sortedPlotProfiles;
}


sub makeAndSetPlots {
  my ($self, $plotParts, $hasStdError) = @_;
  my @rv;
  
  my $bottomMarginSize = $self->getBottomMarginSize();
  my $colors= $self->getProfileColors();
  my $pctColors= $self->getPercentileColors();
  my $sampleLabels = $self->getSampleLabels();

  foreach my $key (sort {$self->sortKeys($a, $b)} keys %$plotParts) {
    my @plotProfiles =  @{$plotParts->{$key} };
    my @profileSetsArray;

    my $sortedPlotProfiles = $self->orderPlotProfiles(\@plotProfiles);

    foreach my $p (@$sortedPlotProfiles) {
      if ($hasStdError->{ $p->{profileName}} && !($key=~/percentile/)) {
	push @profileSetsArray, [$p->{profileName}, $p->{profileType}, $p->{profileName}, 'standard_error'];
      } else {
	if (defined $p->{displayName}) {
	  push @profileSetsArray, [$p->{profileName}, $p->{profileType}, '','','','','','', $p->{displayName}];
	} else {
	  push @profileSetsArray, [$p->{profileName}, $p->{profileType}];
	}
      }
    }

    my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

    my $xAxisLabel;
    my $exprMetric = $self->getExprMetric();
    if ($exprMetric) {
        $self->setExpressionMetric($exprMetric);
    }
    my $plotObj;
    my $plotPartModule = $key=~/percentile/? 'Percentile': $self->getExprPlotPartModuleString();
    
    if((lc($self->getGraphType()) eq 'bar' || ($key=~/percentile/ && blessed($self) =~/TwoChannel/)) && $self->useLegacy() ) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::LegacyGGBarPlot::$plotPartModule";
    } elsif($key=~/Both_strands/ && $plotPartModule eq 'RNASeq') {
	$self->setWantLogged(1);
	if(lc($self->getGraphType()) eq 'bar') {
	    $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::${plotPartModule}SenseAntisense";
	} elsif(lc($self->getGraphType()) eq 'line') {
	    $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::${plotPartModule}SenseAntisense";
	    $xAxisLabel= $self->getXAxisLabel();
	}	    
    } elsif((lc($self->getGraphType()) eq 'bar' || ($key=~/percentile/ && blessed($self) =~/TwoChannel/)) && !$self->useLegacy() ) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::$plotPartModule";
    } elsif(lc($self->getGraphType()) eq 'line' && $self->useLegacy()) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::LegacyGGLinePlot::$plotPartModule";
      $xAxisLabel= $self->getXAxisLabel();
    } elsif(lc($self->getGraphType()) eq 'line' && !$self->useLegacy()) {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::$plotPartModule";
      $xAxisLabel= $self->getXAxisLabel();
    } elsif(lc($self->getGraphType()) eq 'scatter') {
      $plotObj = "EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot::LogRatio";
      $xAxisLabel= $self->getXAxisLabel();
    } else {
      die "Graph must define a graph type of bar or line";
    }

    my $profile = eval {
      $plotObj->new($self,$profileSets);
    };

    if ($@) {
      die "Unable to make plot $plotObj: $@";
    }

    if ($key!~/Both_strands/) {
       $profile->setProfileSets($profileSets);
       my @legendNames = map { $self->getRemainderNameFromProfileSetName($_->[0]) } @profileSetsArray;
       # omit the legend when there is just one profile, and it is not a RNA-Seq dataset
       my $keepSingleLegend = $self->keepSingleLegend();
       if  ($#legendNames || $keepSingleLegend) {
          $profile->setHasExtraLegend(1); 
          $profile->setLegendLabels(\@legendNames);
       }
    }
    

    my $profile_part_name = $profile->getPartName(); # percentile / rma
    $key =~s/values/$profile_part_name/;
    $key =~s/^\_//;
    $profile->setPartName($key);
    $profile->setPlotTitle("$key - " . $profile->getId() );
    my @profileTypes = map { $_->[1] } @profileSetsArray;
    $profile->setProfileTypes(\@profileTypes);

    if(lc($self->getGraphType()) eq 'bar') {
      $profile->setForceHorizontalXAxis($self->forceXLabelsHorizontal());
    }

    if($bottomMarginSize) {
      $profile->setElementNameMarginSize($bottomMarginSize);
    }

    if($xAxisLabel) {
      $profile->setXaxisLabel($xAxisLabel);
    }
  

    if(@$sampleLabels) {
      $profile->setSampleLabels($sampleLabels);
    }

    # These can be implemented by the subclass if needed
    if ($key=~/percentile/) {
      $profile->setColors($pctColors);
    } 
    elsif ($key=~/Both_strands/) {
      my @colorArray = reverse(@{$colors});
	if (scalar @colorArray == 1) {
	    push @colorArray, "gray";
	}
	$profile->setColors(\@colorArray);
    }
    else {
      $profile->setColors($colors);
    }
    $self->finalProfileAdjustments($profile);
    push @rv, $profile;
  }
  $self->setGraphObjects(@rv);
}

# get the string and make an array
sub excludedProfileSetsArray { 
  my ($self) = @_;

  my $excludedProfileSetsString = $self->excludedProfileSetsString();
  my @rv = split(/;/, $excludedProfileSetsString);

  return \@rv;
}

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  return 0;
}

sub getSampleLabels {
  my ($self) = @_;

  my $sampleLabelsString = $self->getSampleLabelsString();
  my @rv = split(/;/, $sampleLabelsString);

  return \@rv;
}

sub getColors {
  my ($self) = @_;

  my $colorsString = $self->getColorsString();

  if($colorsString) {
    my @rv = split(/;/, $colorsString);
    return \@rv;
  }
  return ['grey'];
}


sub getProfileColors {
  my ($self) = @_;

  return $self->getColors();
}


# one channel just use the same colors
sub getPercentileColors {
  my ($self) = @_;

  return $self->getColors();
}


sub forceXLabelsHorizontal {
  my ($self) = @_;

  if(lc($self->getForceXLabelsHorizontalString()) eq 'true') {
    return 1;
  }
  return 0;
}

1;


## VectorBase ##
package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_787e6c5a6f;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  profile.df.full$LEGEND <- as.factor(profile.df.full$NAME)
  hideLegend = FALSE
RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_1b7a4b6253;

sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;

  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');
  $profileType = 'percentile' if ($profileType eq 'channel2_percentiles');

  return ["${profileSetName}_${profileType}"]; 
}

sub declareParts {
  my ($self) = @_;

  my $arrayRef = $self->SUPER::declareParts();
  my $width = @{$arrayRef}[0]->{width};
  my $height = @{$arrayRef}[0]->{height} * 5;

  my @newParts = ({height => $height, width => $width, visible_part => "bloodmeal_time_series_rma,blood_fed_adult_female_rma,male_versus_female_rma,larval_and_adult_stages_rma,consecutive_blood_meals_rma"},
		  {height => $height, width => $width, visible_part => "bloodmeal_time_series_percentile,blood_fed_adult_female_percentile,male_versus_female_percentile,larval_and_adult_stages_percentile,consecutive_blood_meals_percentile"});

  return \@newParts;
}

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $graphObjects = $self->getGraphObjects();

  my @newGraphObjects;
  my @newOrder = (7,6,8,9,1,0,3,2,5,4);
  @newGraphObjects = @$graphObjects[@newOrder];

  $self->setGraphObjects(@newGraphObjects);
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_f6d53b7d28;
# @Override
sub getKeys{
  my ($self, $profileSetName, $profileType) = @_;

  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');
  $profileType = 'percentile' if ($profileType eq 'channel2_percentiles');

  my $mainKey = ["_${profileType}"];
  if ($profileType eq 'amplitude' || $profileType eq 'period' || $profileType eq 'pvalue') {
    $mainKey = ["_values", "_percentile"];
  }

  return($mainKey);
}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
annotation.df <- profile.df.full[!profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),]
profile.df.full <- profile.df.full[profile.df.full$PROFILE_TYPE %in% c('values', 'channel1_percentiles'),]
profile.df.full$GROUP <- unlist(lapply(strsplit(as.character(profile.df.full$ELEMENT_NAMES), " "), function(x){paste(x[1],x[2])}))
profile.df.full$PROFILE_FILE = profile.df.full$GROUP
profile.df.full$LEGEND = profile.df.full$GROUP
profile.df.full$ELEMENT_NAMES_NUMERIC <- unlist(lapply(strsplit(as.character(profile.df.full$ELEMENT_NAMES), " "), "[", 3))
profile.df.full$ELEMENT_NAMES_NUMERIC <- gsub("ZT", "", profile.df.full$ELEMENT_NAMES_NUMERIC)
profile.df.full$ELEMENT_NAMES_NUMERIC <- gsub("CT", "", profile.df.full$ELEMENT_NAMES_NUMERIC)
profile.df.full$ELEMENT_NAMES_NUMERIC <- as.numeric(profile.df.full$ELEMENT_NAMES_NUMERIC)

if (nrow(annotation.df) > 0) {
  annotation.df <- transform(annotation.df, "VALUE"=ifelse(VALUE < .05 & PROFILE_TYPE == "pvalue", "<0.05", VALUE))
  annotation.df$LEGEND <- trimws(tolower(gsub("circadian", "", annotation.df$LEGEND)))
  annotation.df$LINETEXT <- paste0(substr(annotation.df$ELEMENT_NAMES, 1,3), " ", annotation.df$PROFILE_TYPE, ": ", annotation.df$VALUE)
  annotation.df <- group_by(annotation.df, LEGEND)
  annotation.df <- summarize(annotation.df, LINETEXT = paste(LINETEXT, collapse="||"))
  annotation.df$LINETEXT <- paste0(annotation.df$LEGEND, "||", annotation.df$LINETEXT)
  profile.df.full <- merge(profile.df.full, annotation.df, by = "LEGEND") 
  profile.is.numeric <- TRUE
}
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

  $profile->addAdjustProfile($rAdjustString);
  $profile->setRPostscript($rPostscript);
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_38a3c29f33;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  profile.is.numeric <- FALSE
RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_54e6673e59;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  profile.df.full$ELEMENT_NAMES_NUMERIC <- c('0','10','20','30','0','10','20','30')
  profile.is.numeric <- TRUE
RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_27ce39853f;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  profile.df.full$NAME <- gsub("gravid female:", "", profile.df.full$NAME, fixed=T)
  profile.df.full$NAME <- gsub("virgin female:", "", profile.df.full$NAME, fixed=T)
  profile.df.full$NAME <- gsub("4th instar larvae:", "", profile.df.full$NAME, fixed=T)
RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_d4fcd31fd0;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
  profile.df.full$ELEMENT_NAMES_NUMERIC <- as.numeric(gsub("h mosquito embryo", "", profile.df.full$ELEMENT_NAMES))
  profile.is.numeric <- TRUE
RADJUST

  $profile->addAdjustProfile($rAdjustString);
}
1;

#package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_2110a8010e;
#
#sub finalProfileAdjustments {
#  my ($self, $profile) = @_;
#
#  my $rAdjustString = << 'RADJUST';
#  profile.df.full$LEGEND <- unlist(lapply(strsplit(as.character(profile.df.full$NAME), "_"), "[", 1))
#  profile.df.full$NAME <- gsub("Mated_", "", profile.df.full$NAME)
#  profile.df.full$NAME <- gsub("Virgin_", "", profile.df.full$NAME)
#  hide
#RADJUST
#
#  $profile->addAdjustProfile($rAdjustString);
#}
#1;
#

### PlasmoDB ###
package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_6d6cf09eae;
sub getGroupRegex {
  return 'winzeler';
}
sub getRemainderRegex {
  return 'winzeler_(.+)';
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_84d52f99c7;
sub getGroupRegex {
  return ' (\S+) Derived';
}
sub getRemainderRegex {
  return '(Hour \d+)';
}
sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /^Profiles of /){
    return 1;
  }
  return 0;
} 

1;



package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_0fa4237b4b;


use Data::Dumper;

sub getAllProfileSetNames {
  my ($self) = @_;
  my @profileArray = (
                      'Llinas RT transcription and decay labeled Profiles',
                      'Llinas RT transcription and decay unlabeled Profiles',
                      'Llinas RT transcription and decay total Profiles',
      );

  my @rv;
  foreach(@profileArray) {
    my $profileType = 'values';
    my $p = {profileName=>$_, profileType=>$profileType};
    push @rv, $p;
  }

  return \@rv;
}


sub getRemainderNameFromProfileSetName {
  my ($self, $profileSetName) = @_;
  my $remainder =   $self->SUPER::getRemainderNameFromProfileSetName($profileSetName);

  my $map = {'total' => 'Total Abundance', 'labeled' => 'Transcription', 'unlabeled' => 'Stabilization'};

  return $map->{$remainder};
}

sub getRemainderRegex {
  return qr/Llinas RT transcription and decay (.+) Profiles/;
}


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $pch = ['15','NA'];
  my $colors = ['black'];
  my $legend = ['Total Expression', 'Total Expression - smoothed'];
  
  my @profileArray = (
                      ['Llinas RT transcription and decay total Profiles - loess', 'values'],
#                      ['Llinas RT transcription and decay total Profiles - smoothed', 'values']
      );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
 
  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $line->setProfileSets($profileSets);
  $line->setPartName('exprn_val_log_ratio');
  $line->setYaxisLabel('Expression Values (log2 ratio)');
  $line->setPointsPch($pch);
  $line->setColors([$colors->[0]]);
  $line->setArePointsLast(1);
  $line->setElementNameMarginSize(6);
  $line->setXaxisLabel('Hours post infection');
  $line->setHasExtraLegend(1);
  $line->setSmoothLines(1);
  $line->setSmoothWithLoess(1);
  $line->setLegendLabels(['total']);
  $line->setXaxisLabel('Hours post infection');
  my $id = $self->getId();
  $line->setPlotTitle("Expression Values - $id - Total mRNA Abundance");

  my $graphObjects = $self->getGraphObjects();
  
  my $dynamics = $graphObjects->[0];
  my $baseTitle = $dynamics->getPlotTitle();
  $dynamics->setPointsPch([ 'NA', 'NA', 'NA']);
  $dynamics->setColors(['red','black','blue']);
  $dynamics->setHasExtraLegend(1);
  $dynamics->setPlotTitle($baseTitle. " - mRNA Dynamics");
  $dynamics->setYaxisLabel('Modeled Expression Values');

  push @$graphObjects, $line;

  $self->setGraphObjects(@$graphObjects);
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_307a1b10a9;
sub getGroupRegex {
  return 'ZB Pvivax Time Series';
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_1556ad1e1e;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $colors = $profile->getColors();


  my @allLegend;
  foreach(1..8) {
    push @allLegend, "mild disease";
  }
  foreach(1..9) {
    push @allLegend, "severe disease";
  }
  $profile->setColors([$colors->[0], $colors->[1]]);

#  $profile->setLegendColors([$colors->[0], $colors->[1]]);
#  my $legend = ['rep("mild disease",8)', 'rep("severe disease", 9)'];
  $profile->setHasExtraLegend(1); 
  $profile->setLegendLabels(\@allLegend);

}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_4582562a4b;

sub finalProfileAdjustments {                                                                                
  my ($self, $profile) = @_;
  my $rAdjustString = << 'RADJUST';    
     profile.df.full$NAME = factor(profile.df.full$NAME, levels = c("Sporozoite","Early Ring","Late Ring","Early Trophozoite","Late Trophozoite","Early Schizogony","Late Schizogony","Merozoite","Gametocyte"));
RADJUST

  $profile->addAdjustProfile($rAdjustString);

}

sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my @winzelerProfileArray = (['winzeler_cc_sorbExp','values'],
                              ['winzeler_cc_tempExp', 'values'],
                              ['winzeler_cc_sexExp', 'values']
                             );

  my @colors = ('brown', 'cyan', 'purple' );

  my $winzelerProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@winzelerProfileArray);

  my $winzeler = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $winzeler->setProfileSets($winzelerProfileSets);
  $winzeler->setColors(\@colors);
  $winzeler->setPartName('line');
  $winzeler->setPointsPch([15,15,15]);
  $winzeler->setArePointsLast(1);
  $winzeler->setAdjustProfile('
     profile.df.full$ELEMENT_NAMES = factor(c("Early Ring","Late Ring","Early Trophozoite","Late Trophozoite","Early Schizogony","Late Schizogony","Merozoite","Early Ring","Late Ring","Early Trophozoite","Late Trophozoite","Early Schizogony","Late Schizogony","Merozoite","Sporozoite","Gametocyte"), levels = c("Sporozoite","Early Ring","Late Ring","Early Trophozoite","Late Trophozoite","Early Schizogony","Late Schizogony","Merozoite","Gametocyte"))
     profile.df.full$GROUP = factor(c("C","C","C","C","C","C","D","E","E","E","E","E","E","F","A","B"), levels=c("A", "B", "C", "D", "E", "F")) ');
     
  $winzeler->setXaxisLabel('');

  my $graphObjects = $self->getGraphObjects();
  my @legendNames = ('Mosquito and sexual stages','Sorbitol synchronized','Temperature synchronized');
  foreach my $graphObject (@$graphObjects) {
      $graphObject->setHasExtraLegend(1);
      $graphObject->setLegendLabels(\@legendNames);
  }

  push @$graphObjects, $winzeler;

  $self->setGraphObjects(@$graphObjects);

}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_3ef554e244;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $colors = ['green', 'blue', 'red'];

  $profile->setIsHorizontal(1);

  $profile->setColors($colors);

  my @allLegend;

  foreach(1..4) {
    push @allLegend, "SIR KO";
  }
  foreach(1..3) {
    push @allLegend, "red cell receptor invasion";
  }
  foreach(1..5) {
    push @allLegend, "merozoite invasion";
  }



#  my $legend = ['merozoite invasion', 'SIR KO', 'red cell receptor invasion'];
  my $legend = \@allLegend;
#  $profile->setHasExtraLegend(1); 
  $profile->setLegendLabels($legend);

}
1;




package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_3f06ca816e;

# LAST RESORT IS TO OVERRIDE THE INIT METHOD
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#F08080', '#7CFCB0' ];
  my $legend = ['untreated', 'chloroquine'];

  my @profileArray = (['E-GEOD-10022 array from Su','values'],
                      ['E-GEOD-10022 array from Su', 'values']
                     );

  my @percentileArray = (['E-GEOD-10022 array from Su', 'channel1_percentiles'],
                         ['E-GEOD-10022 array from Su', 'channel1_percentiles'],
                        );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);
  $rma->setHasExtraLegend(1); 
  $rma->setLegendLabels($legend);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setHasExtraLegend(1); 
  $percentile->setLegendLabels($legend);

  my $rAdjustString = << 'RADJUST';
profile.df.full$NAME <- gsub(" no CQ", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub(" CQ", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub("106-1", "106/1", profile.df.full$NAME)

RADJUST

  $rma->addAdjustProfile($rAdjustString);
  $percentile->addAdjustProfile($rAdjustString);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}

1;



package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_7349a4c6a5;

# LAST RESORT IS TO OVERRIDE THE INIT METHOD
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#6495ED', '#E9967A', '#2F4F4F' ];
  my $legend = ['Wild Type', 'sir2A', 'sir2B'];

  my @profileArray = (['Profiles of E-TABM-438 from Cowman', 'values'],
                     );

  my @percentileArray = (['Profiles of E-TABM-438 from Cowman', 'channel1_percentiles'],
                        );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);
  $rma->setHasExtraLegend(1); 
  $rma->setLegendLabels($legend);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setHasExtraLegend(1); 
  $percentile->setLegendLabels($legend);

  my $rAdjustString = << 'RADJUST';
profile.df.full$NAME <- gsub("wild type - ", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub("sir2a KO - ", "", profile.df.full$NAME)
profile.df.full$NAME <- gsub("sir2b KO - ", "", profile.df.full$NAME)
profile.df.full$NAME <- factor(profile.df.full$NAME, levels=unique(profile.df.full$NAME))
profile.df.full$LEGEND <- gsub(" - trophozoite", "", profile.df.full$LEGEND)
profile.df.full$LEGEND <- gsub(" - schizont", "", profile.df.full$LEGEND)
profile.df.full$LEGEND <- gsub(" - ring", "", profile.df.full$LEGEND)
profile.df.full$LEGEND <- gsub(" KO", "", profile.df.full$LEGEND)
profile.df.full$LEGEND <- factor(profile.df.full$LEGEND, levels=unique(profile.df.full$LEGEND))
hideLegend = FALSE

RADJUST

  $rma->addAdjustProfile($rAdjustString);
  $percentile->addAdjustProfile($rAdjustString);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_c6622915ff;

#TODO
sub _init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['purple', 'darkred', 'green', 'orange']; # as in the paper!
  my $pch = [19,24,20,23];
  my $legend = ['Ring', 'Trophozoite', 'Schizont', 'Late schiz.'];


  my @profileArray = (['DeRisi_HalfLife', 'values', '', ''],
                     );


  my $id = $self->getId();

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $hl = EbrcWebsiteCommon::View::GraphPackage::BarPlot->new(@_);
  $hl->setProfileSets($profileSets);
  $hl->setColors($colors);
  $hl->setForceHorizontalXAxis(1);
  $hl->setPartName('half_life');
  $hl->setHighlightMissingValues(1);
  $hl->setYaxisLabel('half-life (min)');
  $hl->setPlotTitle("Half-life - $id");


  my @profileArrayLine = (['Profiles of Derisi HalfLife-Ring', 'values', '', ''],
                          ['Profiles of Derisi HalfLife-Trophozoite', 'values', '', ''],
                          ['Profiles of Derisi HalfLife-Schizont', 'values', '', ''],
                          ['Profiles of Derisi HalfLife-Late_Schizont', 'values', '', '']
                         );
  
  my $profileSetsLine = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayLine);

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $line->setPartName('expr_val');
  $line->setProfileSets($profileSetsLine);
  $line->setColors($colors);
  $line->setYaxisLabel('half-life (min)');
  $line->setPlotTitle("Expression Normalized to 0 Hour - $id");
  $line->setPointsPch($pch);
  $line->setDefaultYMax(1);
  $line->setDefaultYMin(0);

  # R code normalizes to the 0HR Timepoint then filters away the Control Sample
  # Could have done the filtering by passing an array to "makeProfileSets" foreach of the profiles
  $line->setAdjustProfile("for(i in 1:nrow(lines.df)) { lines.df[i,] = 2^lines.df[i,]/2^lines.df[i,2]};lines.df = lines.df[,2:ncol(lines.df)];points.df = points.df[,2:ncol(points.df)];"); 

  $self->setGraphObjects($hl, $line);
  return $self;
}

1;




### ToxoDB ###
package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_e8c4cf2187;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;
#  $profile->addAdjustProfile('profile.df = cbind(profile.df[,1], profile.df[,3], profile.df[,2], profile.df[,4]);');

  my $colors = $profile->getColors();
  my @elementNames = ("WT:Stressed","WT:Unstressed","KO:Stressed","KO:Unstressed");

  my $legendLabels = ["Wild Type","Wild Type", "GCN5-A Knockout","GCN5-A Knockout" ];
  $profile->setColors(["#D87093","#87CEEB"]);

  $profile->setSampleLabels(\@elementNames);

  $profile->setLegendLabels($legendLabels);
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_2daab8c933;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  my $colors = ['#B22222','#6A5ACD','#87CEEB' ];
  my $legend = ['GT1', 'ME49', 'CTGara'];

  my $rAdjustString = << 'RADJUST';
  profile.df.full$LEGEND <- unlist(lapply(strsplit(as.character(profile.df.full$NAME), " "), "[", 1))
  profile.df.full$LEGEND <- as.factor(profile.df.full$LEGEND)
  profile.df.full$NAME <- gsub("GT1 ", "", profile.df.full$NAME)
  profile.df.full$NAME <- gsub("ME49 ", "", profile.df.full$NAME)
  profile.df.full$NAME <- gsub("CTGara ", "", profile.df.full$NAME)
  hideLegend = FALSE
RADJUST

  my $profileName ="three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditions";

  my @profileArray = (["$profileName", "values", "$profileName", "standard_error"]);

  my @percentileArray = (["$profileName", "channel1_percentiles"]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);
  $rma->setHasExtraLegend(1); 
  $rma->setLegendLabels($legend);
  $rma->addAdjustProfile($rAdjustString);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setHasExtraLegend(1);
  $percentile->setLegendLabels($legend);
  $percentile->addAdjustProfile($rAdjustString);
 
  $self->setGraphObjects($rma, $percentile);

  return $self;
}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_2750122e82;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  #my $legend = ['oocyst','oocyst','oocyst', 'tachyzoite','bradyzoite','bradyzoite','bradyzoite'];
  my $rAdjustString = << 'RADJUST';
  profile.df.full$LEGEND <- c("oocyst", "oocyst", "oocyst", "tachyzoite", "bradyzoite", "bradyzoite", "bradyzoite");
  profile.df.full$LEGEND <- as.factor(profile.df.full$LEGEND)
  hideLegend = FALSE;
RADJUST

  $profile->addAdjustProfile($rAdjustString);
  $profile->setColors(["#D87093","#E9967A","#87CEEB"]);
#  $profile->setLegendLabels($legend);

  return $self;
}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_994d646c6a;
# LAST RESORT IS TO OVERRIDE THE INIT METHOD
sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $colorsRoos = ['#4682B4','#6B8E23','#00FF00','#2E8B57'];
  my $colorsFlo  = ['#CD853F','#8FBC8F'];
  my $graphs;

  my @profileArrayRoos = (['expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)', 'values', '', ''],
 			  ['expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions', 'values', '', ''],
 			  ['expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions', 'values', '', ''],
 			  ['expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', 'values', '', '']
			  );

   my @profileArrayFlo = (['expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions : 2-14 days (by Florence Dzierszinski)', 'values', '', ''],
                          ['expression profiles of VEG strain CO2-starvation bradyzoite inducing conditions : 2-6 days (by Florence Dzierszinski)', 'values', '', '']
                         );

   my @percentileArrayRoos = (['expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)', 'channel1_percentiles', '', ''],
 			     ['expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions', 'channel1_percentiles', '', ''],
 			     ['expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions', 'channel1_percentiles', '', ''],
 			     ['expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', 'channel1_percentiles', '', '']
 			    );

   my @percentileArrayFlo = (['expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions : 2-14 days (by Florence Dzierszinski)', 'channel1_percentiles', '', ''],
 			    ['expression profiles of VEG strain CO2-starvation bradyzoite inducing conditions : 2-6 days (by Florence Dzierszinski)', 'channel1_percentiles', '', '']
 			   );

  my $id = $self->getId();

  my $profileSetsRoos = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayRoos);

  my $rma =  EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $rma->setProfileSets($profileSetsRoos);
  $rma->setPartName('Roos_RMA');
  $rma->setYaxisLabel('RMA Value (log2)');
  $rma->setColors($colorsRoos);
  $rma->setElementNameMarginSize(4);
  $rma->setHasExtraLegend(1);
  $rma->setLegendLabels(['Pru Alk', 'Pru CO2', 'Pru NA', 'RH Alk']);
  $rma->setXaxisLabel('Hours post infection');
  $rma->setPlotTitle("0-72 hours RMA Expression Value - $id");
  $rma->setDefaultYMin(0);
  push (@{$graphs},$rma);
  $self->SUPER::setGraphObjects(@{$graphs});

   my $percentileSetsRoos = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArrayRoos);

  my $percentileRoos = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $percentileRoos->setProfileSets($percentileSetsRoos);
  $percentileRoos->setPartName('Roos_percentile');
  $percentileRoos->setYaxisLabel('Percentile');
  $percentileRoos->setColors($colorsRoos);
  $percentileRoos->setElementNameMarginSize(4);
  $percentileRoos->setHasExtraLegend(1);
  $percentileRoos->setLegendLabels(['Pru Alk', 'Pru CO2', 'Pru NA', 'RH Alk']);
  $percentileRoos->setXaxisLabel('Hours post infection');
  $percentileRoos->setPlotTitle("0-72 hours Percentile - $id");
  $percentileRoos->setDefaultYMax(100);
  push (@{$graphs},$percentileRoos);
  $self->SUPER::setGraphObjects(@{$graphs});


  my $profileSetsFlo = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArrayFlo);

  my $rmaFlo =  EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $rmaFlo->setProfileSets($profileSetsFlo);
  $rmaFlo->setPartName('Dzierszinskis_RMA');
  $rmaFlo->setYaxisLabel('RMA Value (log2)');
  $rmaFlo->setColors($colorsFlo);
  $rmaFlo->setElementNameMarginSize(4);
  $rmaFlo->setHasExtraLegend(1);
  $rmaFlo->setLegendLabels(['Pru Co2', 'VEG CO2']);
  $rmaFlo->setXaxisLabel('Days post infection');
  $rmaFlo->setPlotTitle("2-14 days RMA Expression Value - $id");
  $rmaFlo->setDefaultYMin(0);
  push (@{$graphs},$rmaFlo);
  $self->SUPER::setGraphObjects(@{$graphs});

   my $percentileSetsFlo = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArrayFlo);

  my $percentileFlo = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $percentileFlo->setProfileSets($percentileSetsFlo);
  $percentileFlo->setPartName('Flo_percentile');
  $percentileFlo->setYaxisLabel('Percentile');
  $percentileFlo->setColors($colorsFlo);
  $percentileFlo->setElementNameMarginSize(4);
  $percentileFlo->setHasExtraLegend(1);
  $percentileFlo->setLegendLabels(['Pru CO2', 'VEG CO2']);
  $percentileFlo->setXaxisLabel('Days post infection');
  $percentileFlo->setPlotTitle("2-14 days Percentile - $id");
  $percentileFlo->setDefaultYMax(100);
  push (@{$graphs},$percentileFlo);
  $self->SUPER::setGraphObjects(@{$graphs});

}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_35bb13db5b;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  my $legend = ['11hr_Egress','Time_Series','WildType_V_Mutant'];

  $profile->setHasExtraLegend(1);
  $profile->setLegendLabels($legend);
  
  $self->setPlotWidth(600); 

  return $self;
}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_73d06a9e7b;
sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  if ($psName =~ /Expression profiling of the 3 archetypal T. gondii lineages/){
    return 1;
  }
  return 0;
}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_c1a3dbb014;

# LAST RESORT IS TO OVERRIDE THE INIT METHOD
sub init {
  my $self = shift;
  $self->SUPER::init(@_);
  my @profileSet=(['M.White Cell Cycle Microarray', 'values', '', ''],);
  my @percentileSet=(['M.White Cell Cycle Microarray', 'channel1_percentiles', '', ''],);

  my $colors = ['#CD853F'];
  my $graphs;
  my $id = $self->getId();
 #this for ggplot with straight lines
  my $cellCycleAnnotation = "
gp = gp + annotate(\"segment\", x = 2, xend = 5.75, y = min(profile.df.full\$VALUE) - 1, yend = min(profile.df.full\$VALUE) - 1 , colour = '#d3883f');
gp = gp + annotate(\"text\", x = 4, y = min(profile.df.full\$VALUE) - 1.75, label = \"S(1)\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 5, xend = 6.9, y = min(profile.df.full\$VALUE) - 1.35, yend = min(profile.df.full\$VALUE) - 1.35, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 5.5, y = min(profile.df.full\$VALUE) - 2.1, label = \"M\", colour = '#d3883f');
gp = gp + annotate(\"text\", x = 6.5, y = min(profile.df.full\$VALUE) - 2.1, label = \"C\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 6.1, xend = 10.4, y = min(profile.df.full\$VALUE) - 1, yend = min(profile.df.full\$VALUE) - 1, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 8.375, y = min(profile.df.full\$VALUE) - 1.75, label = \"G1\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 10, xend = 13.2, y = min(profile.df.full\$VALUE) - 1.35, yend = min(profile.df.full\$VALUE) - 1.35, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 11.5, y = min(profile.df.full\$VALUE) - 2.1, label = \"S(2)\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 12, xend = 14, y = min(profile.df.full\$VALUE) - 1, yend = min(profile.df.full\$VALUE) - 1, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 12.5, y = min(profile.df.full\$VALUE) - 1.75, label = \"M\", colour = '#d3883f');
gp = gp + annotate(\"text\", x = 13.5, y = min(profile.df.full\$VALUE) - 1.75, label = \"C\", colour = '#d3883f');
";
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSet);
  my $rma =  EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setPartName('rma');
  $rma->setColors($colors);
  $rma->setSmoothLines(1);
  $rma->setSplineApproxN(61);
  $rma->setDefaultYMax(10);
  $rma->setDefaultYMin(0);
  $rma->setElementNameMarginSize(6.4);
  $rma->setTitleLine(2.25);
  $rma->setRPostscript($cellCycleAnnotation);
  $rma->setXaxisLabel('Time point (hours)');
  $rma->setYaxisLabel('RMA Value (log2)');
  $rma->setPlotTitle("RMA Expression Value - $id");
  $rma->addAdjustProfile('profile.df.full$ELEMENT_NAMES = factor(profile.df.full$ELEMENT_NAMES, levels=c("asynchronous","0 HR","1 HR","2 HR","3 HR","4 HR","5 HR","6 HR","7 HR","8 HR","9 HR","10 HR","11 HR","12 HR"));
      profile.df.full$GROUP = c("A","C","C","C","C","C","C","C","C","C","C","C","C","C");
      profile.df.full$STAGE = as.factor(c("none","S(1)","S(1)","S(1)","M","C","G1","G1","G1","S(2)","S(2)","M","C","C"));');

#this below for ggplot2
  my $PercentileCellCycleAnnotation = "
gp = gp + annotate(\"segment\", x = 2, xend = 5.75, y = min(profile.df.full\$VALUE) - 10, yend = min(profile.df.full\$VALUE) - 10 , colour = '#d3883f');
gp = gp + annotate(\"text\", x = 4, y = min(profile.df.full\$VALUE) - 17.5, label = \"S(1)\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 5, xend = 6.9, y = min(profile.df.full\$VALUE) - 13.5, yend = min(profile.df.full\$VALUE) - 13.5, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 5.5, y = min(profile.df.full\$VALUE) - 21, label = \"M\", colour = '#d3883f');
gp = gp + annotate(\"text\", x = 6.5, y = min(profile.df.full\$VALUE) - 21, label = \"C\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 6.1, xend = 10.4, y = min(profile.df.full\$VALUE) - 10, yend = min(profile.df.full\$VALUE) - 10, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 8.375, y = min(profile.df.full\$VALUE) - 17.5, label = \"G1\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 10, xend = 13.2, y = min(profile.df.full\$VALUE) - 13.5, yend = min(profile.df.full\$VALUE) - 13.5, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 11.5, y = min(profile.df.full\$VALUE) - 21, label = \"S(2)\", colour = '#d3883f');
gp = gp + annotate(\"segment\", x = 12, xend = 14, y = min(profile.df.full\$VALUE) - 10, yend = min(profile.df.full\$VALUE) - 10, colour = '#d3883f');
gp = gp + annotate(\"text\", x = 12.5, y = min(profile.df.full\$VALUE) - 17.5, label = \"M\", colour = '#d3883f');
gp = gp + annotate(\"text\", x = 13.5, y = min(profile.df.full\$VALUE) - 17.5, label = \"C\", colour = '#d3883f');
";

  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSet);
  my $percentile =  EbrcWebsiteCommon::View::GraphPackage::GGLinePlot->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setPartName('percentile');
  $percentile->setColors($colors);
  $percentile->setSmoothLines(1);
  $percentile->setSplineApproxN(61);
  $percentile->setElementNameMarginSize(6.4);
  $percentile->setTitleLine(2.25);
  $percentile->setRPostscript($PercentileCellCycleAnnotation);
  $percentile->setXaxisLabel('Time point (hours)');
  $percentile->setYaxisLabel('Percentile');
  $percentile->setPlotTitle("Percentile - $id");
  $percentile->addAdjustProfile('profile.df.full$ELEMENT_NAMES = factor(profile.df.full$ELEMENT_NAMES, levels=c("asynchronous","0 HR","1 HR","2 HR","3 HR","4 HR","5 HR","6 HR","7 HR","8 HR","9 HR","10 HR","11 HR","12 HR"));
      profile.df.full$GROUP = c("A","C","C","C","C","C","C","C","C","C","C","C","C","C");');


  $self->SUPER::setGraphObjects($rma, $percentile);

}

# sub isExcludedProfileSet {
#   my ($self, $psName) = @_;

#   foreach(@{$self->excludedProfileSetsArray()}) {
#     return 1 if($_ eq $psName);
#   }
#   if ($psName =~ /M.White Cell Cycle Microarray spline smoothed/){
#     return 1;
#   }
#   return 0;
# }

1;



### TriTrypDB ###

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_b7dc54ebad;

sub finalProfileAdjustments {                                                                                
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';    
    profile.df.full$ELEMENT_NAMES = factor(profile.df.full$ELEMENT_NAMES, levels=c("Slender","0 hr","1 hr","6 hr","18 hr","48 hr"));
    profile.df.full$GROUP = c("A","C","C","C","C","C");
RADJUST

  $profile->addAdjustProfile($rAdjustString);

  $profile->setXaxisLabel('');
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_04880972f5;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  my $legend = ['biorep01','biorep02'];

  $profile->setHasExtraLegend(1);
  $profile->setLegendLabels($legend);
  return $self;
}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_2bf995383e;
use vars qw( @ISA );
@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use strict;
sub getGraphType { 'bar' }
sub getExprPlotPartModuleString { 'Standardized' }

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#E9967A', '#8B4513','#66CDAA', '#556B2F', '#87CEEB','#008080', '#C9BE62'];

  my $xAxisLabels = ['2 Hrs', '6 Hrs','12 Hrs', '24 Hrs','36 Hrs','48 Hrs','72 Hrs'];

  my @profileArray = (['Cparvum_RT_PCR_Kissinger', 'values', '', '', $xAxisLabels ]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $standardized = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Standardized->new(@_);
  $standardized->setProfileSets($profileSets);
  $standardized->setColors($colors);
  $standardized->setForceHorizontalXAxis(1);

  $self->setGraphObjects($standardized);

  return $self;
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_57b027468b;
use vars qw( @ISA );
@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use strict;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  $self->setPlotWidth(500);

  return $self;
}
1;

# VectorBase agamPEST_microarrayExpression_E-MEXP-3078_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_f51b6797d0;
use vars qw( @ISA );
@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use strict;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  $self->setPlotWidth(600);

  return $self;
}
1;



# package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_4582562a4b;
# use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
# use strict;
# sub getGraphType { 'bar' }
# sub excludedProfileSetsString { '' }
# sub getSampleLabelsString { '' }
# sub getColorsString { ''  } 
# sub getForceXLabelsHorizontalString { '' } 
# sub getBottomMarginSize {  }
# sub getExprPlotPartModuleString { 'RMA' }
# sub getXAxisLabel { '' }
#1;

#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR microarraySimpleRmaGraph
# TEMPLATE_ANCHOR microarraySimpleQuantileGraph
# TEMPLATE_ANCHOR microarrayMRNADecayGraph

