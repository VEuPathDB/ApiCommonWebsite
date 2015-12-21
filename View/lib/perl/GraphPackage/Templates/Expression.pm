package ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::Util;

use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::LinePlot;
use ApiCommonWebsite::View::GraphPackage::ScatterPlot;

use Data::Dumper;

# Subclasses can adjust the RCode but we won't let the templates do this
sub getPercentileRAdjust {}
sub getProfileRAdjust {}

sub finalProfileAdjustments {}

# Template subclasses need to implement this....should return 'bar' or 'line'
sub getGraphType {}

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


sub getKey{
  my ($self, $profileSetName, $profileType) = @_;

  my $groupName = $self->getGroupNameFromProfileSetName($profileSetName);
#print STDERR "groupName = $groupName FOR $profileSetName\n";
  $groupName = '' if (!$groupName);
  $profileType = 'percentile' if ($profileType eq 'channel1_percentiles');
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


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $allProfileSets = $self->getAllProfileSetNames();
  my %plotParts;
  my %hasStdError;


  foreach my $p (@{$allProfileSets}){
    my $profileName = $p->{profileName};
    my $profileType = $p->{profileType};
    my $key = $self->getKey($profileName, $profileType);
    push @{$plotParts{$key}}, $p;
    if ($profileType eq 'standard_error') {
     $hasStdError{$profileName} = 1;
   }
  }



  $self->makeAndSetPlots(\%plotParts, \%hasStdError);

  return $self;
}

sub getAllProfileSetNames {
  my ($self) = @_;

  my $datasetId = $self->getDatasetId();

  my $id = $self->getId();

  my $dbh = $self->getQueryHandle();

  my $sql = ApiCommonWebsite::View::GraphPackage::Util::getProfileSetsSql();


  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetId);

  my @rv = ();

  while(my ($profileName, $profileType) = $sh->fetchrow_array()) {
    next if($self->isExcludedProfileSet($profileName));
    my $p = {profileName=>$profileName, profileType=>$profileType};
    push @rv, $p;
  }
  $sh->finish();
  return \@rv;
}

sub sortKeys {
  my ($a_suffix, $a_type) = split("\_", $a);
  my ($b_suffix, $b_type) = split("\_", $b);
  return ($b_type cmp $a_type)  && ($a_suffix cmp $b_suffix);

}

sub makeAndSetPlots {
  my ($self, $plotParts, $hasStdError) = @_;
  my @rv;

  my $bottomMarginSize = $self->getBottomMarginSize();
  my $colors= $self->getColors();
  my $pctColors= $self->getPercentileColors();
  my $sampleLabels = $self->getSampleLabels();

  foreach my $key (sort sortKeys keys %$plotParts) {
    my @plotProfiles =  @{$plotParts->{$key} };
    my @profileSetsArray;

#print STDERR Dumper   \@plotProfiles;
    my @sortedPlotProfiles = sort {$a->{profileName} cmp $b->{profileName} } @plotProfiles;
#print STDERR Dumper   \@sortedPlotProfiles;
    foreach my $p (@sortedPlotProfiles) {
      if ($hasStdError->{ $p->{profileName} }) {
	push @profileSetsArray, [$p->{profileName}, $p->{profileType}, $p->{profileName}, 'standard_error'];
      } else {
	push @profileSetsArray, [$p->{profileName}, $p->{profileType}];
      }
    }
#print STDERR Dumper   \@profileSetsArray;

    my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

    my $xAxisLabel;
    my $plotObj;
    my $plotPartModule = $key=~/percentile/? 'Percentile': $self->getExprPlotPartModuleString();

    if(lc($self->getGraphType()) eq 'bar') {
      $plotObj = "ApiCommonWebsite::View::GraphPackage::BarPlot::$plotPartModule";
    } elsif(lc($self->getGraphType()) eq 'line') {
      $plotObj = "ApiCommonWebsite::View::GraphPackage::LinePlot::$plotPartModule";
      $xAxisLabel= $self->getXAxisLabel();
    } elsif(lc($self->getGraphType()) eq 'scatter') {
      # TODO: handle two channel graphs in a different module
      $plotObj = "ApiCommonWebsite::View::GraphPackage::ScatterPlot::LogRatio";
      $xAxisLabel= $self->getXAxisLabel();
    } else {
      die "Graph must define a graph type of bar or line";
    }
    my $profile = eval {
      $plotObj->new($self);
    };

    if ($@) {
      die "Unable to make plot $plotObj: $@";
    }

    my $profile_part_name = $profile->getPartName(); # percentile / rma
    $key =~s/values/$profile_part_name/;
    $key =~s/^\_//;
    $profile->setPartName($key);

    $profile->setPlotTitle("$key - " . $profile->getId() );


    my @legendNames = map { $self->getRemainderNameFromProfileSetName($_->[0]) } @profileSetsArray;

    # omit the legend when there is just one profile
    if  ($#legendNames) {
      $profile->setHasExtraLegend(1); 
      $profile->setLegendLabels(\@legendNames);
    }

    if(lc($self->getGraphType()) eq 'bar') {
      $profile->setForceHorizontalXAxis($self->forceXLabelsHorizontal());
    }

    $profile->setProfileSets($profileSets);

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
    } else {
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
  return ['blue', 'grey'];
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

sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  
  my $legendLabels = (['labeled','total','total fitted','unlabeled']);
  $profile->setPointsPch([ 'NA', 'NA', 'NA', 'NA']);
  $profile->setHasExtraLegend(1);
  $profile->setLegendLabels($legendLabels);
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_307a1b10a9;
sub getGroupRegex {
  return 'ZB Pvivax Time Series';
}
#sub getRemainderRegex {
#  return  'Patient ';
#}

sub setGraphObjects { 
  my $self = shift;
  my $graphs = [];
  
  my $legendLabels = (['labeled','total','total fitted','unlabeled']);
  foreach my $plotPart (@_) {
    my $name = $plotPart->setHasExtraLegend(1);
    my $size = $plotPart->setLegendLabels($legendLabels);


    push @{$graphs}, $plotPart;
  }

  my $pch = ['NA'];
  my $colors = ['black'];
  my $legend = ['Total Expression'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 2});
  
  my @profileArray = (
                      ['Llinas RT transcription and decay total Profiles - loess'],
                     );


  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
 
  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0]]);
  $line->setPartName('exprn_val_log_ratio');
  $line->setYaxisLabel('Expression Values (log2 ratio)');
  $line->setPointsPch($pch);
  $line->setColors([$colors->[0], $colors->[1],$colors->[2], $colors->[3],]);
  $line->setArePointsLast(1);
  $line->setElementNameMarginSize(6);
  $line->setXaxisLabel('Hours post infection');
  my $id = $self->getId();
  my $basePlotTitle = $line->getPlotTitle;
  $line->setPlotTitle("$basePlotTitle - $id - Time Course");
  push (@{$graphs},$line);
  $self->SUPER::setGraphObjects(@{$graphs});
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_1556ad1e1e;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $colors = $profile->getColors();

  my @allColors;
  foreach(1..8) {
    push @allColors, $colors->[0];
  }
  foreach(1..9) {
    push @allColors, $colors->[1];
  }
  $profile->setColors(\@allColors);
}


1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_4582562a4b;


sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  $profile->addAdjustProfile('profile.df = cbind(profile.df[,1], profile.df[,3:9], profile.df[,2]);');

  my @winzelerNames = ("S", "ER","LR", "ET", "LT","ES", "LS", "M", "G"); 
  $profile->setSampleLabels(\@winzelerNames);
}


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my @tempSorbNames = (2..7, "M");

  my @winzelerNames = ("S", "ER","LR", "ET", "LT","ES", "LS", "M", "G"); 

  my @winzelerProfileArray = (['winzeler_cc_sorbExp','values', '', '', \@tempSorbNames],
                              ['winzeler_cc_tempExp', 'values', '', '', \@tempSorbNames],
                              ['winzeler_cc_sexExp', 'values', '','', [1, 'G']]
                             );

  my @colors = ('cyan', 'purple', 'brown' );

  my $winzelerProfileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@winzelerProfileArray);

  my $winzeler = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $winzeler->setProfileSets($winzelerProfileSets);
  $winzeler->setColors(\@colors);
  $winzeler->setPartName('line');
  $winzeler->setPointsPch([15,15,15]);
  $winzeler->setAdjustProfile('points.df = points.df - mean(points.df[points.df > 0], na.rm=T);lines.df = lines.df - mean(lines.df[lines.df > 0], na.rm=T)');
  $winzeler->setArePointsLast(1);
  $winzeler->setSampleLabels(\@winzelerNames);

  my $graphObjects = $self->getGraphObjects();
  push @$graphObjects, $winzeler;

  $self->setGraphObjects(@$graphObjects);

}
1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_3ef554e244;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $colors = ['green', 'green', 'green', 'green', 'blue', 'blue', 'blue', 'red', 'red', 'red', 'red', 'red'];

  $profile->setIsHorizontal(1);
  $profile->setColors($colors);
}
1;




package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_3f06ca816e;

# LAST RESORT IS TO OVERRIDE THE INIT METHOD
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#F08080', '#7CFC00' ];
  my $legend = ['untreated', 'chloroquine'];
  my $pch = [22];

  my $untreated = ['106/1','','106/1 (76I)','', '106/1 (76I_352K)', ''];
  my $treated = ['', '106/1','','106/1 (76I)','', '106/1 (76I_352K)'];


  my @profileArray = (['E-GEOD-10022 array from Su','values', '', '', $untreated],
                      ['E-GEOD-10022 array from Su', 'values', '', '', $treated]
                     );

  my @percentileArray = (['E-GEOD-10022 array from Su', 'channel1_percentiles', '', '', $untreated],
                         ['E-GEOD-10022 array from Su', 'channel1_percentiles', '', '', $treated],
                        );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileArray);

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);
  $rma->setHasExtraLegend(1); 
  $rma->setLegendLabels($legend);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setHasExtraLegend(1); 
  $percentile->setLegendLabels($legend);

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

  my $wildTypeSamples = ['ring','trophozoite','schizont','','','','','',''];
  my $sir2ASamples = ['','','','ring','trophozoite','schizont','','',''];
  my $sir2BSamples = ['','','', '','','', 'ring','trophozoite','schizont'];

  my @profileArray = (['Profiles of E-TABM-438 from Cowman', 'values', '', '', $wildTypeSamples ],
                      ['Profiles of E-TABM-438 from Cowman', 'values', '', '', $sir2ASamples ],
                      ['Profiles of E-TABM-438 from Cowman', 'values', '', '', $sir2BSamples ],
                     );

  my @percentileArray = (['Profiles of E-TABM-438 from Cowman', 'channel1_percentiles', '', '', $wildTypeSamples],
                         ['Profiles of E-TABM-438 from Cowman', 'channel1_percentiles', '', '', $sir2ASamples],
                         ['Profiles of E-TABM-438 from Cowman', 'channel1_percentiles', '', '', $sir2BSamples],
                        );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileArray);

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);
  $rma->setHasExtraLegend(1); 
  $rma->setLegendLabels($legend);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);
  $percentile->setHasExtraLegend(1); 
  $percentile->setLegendLabels($legend);

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

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $hl = ApiCommonWebsite::View::GraphPackage::BarPlot->new(@_);
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
  
  my $profileSetsLine = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArrayLine);

  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
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
# TEMPLATE_ANCHOR microarraySimpleTwoChannelGraph
