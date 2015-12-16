package ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::Util;

use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

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
print STDERR Dumper   \@sortedPlotProfiles;
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

  $profile->setPointsPch([ 'NA', 'NA', 'NA', 'NA']);
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_307a1b10a9;
sub getGroupRegex {
  return 'ZB Pvivax Time Series';
}
#sub getRemainderRegex {
#  return  'Patient ';
#}

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

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_3ef554e244;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $colors = ['green', 'green', 'green', 'green', 'blue', 'blue', 'blue', 'red', 'red', 'red', 'red', 'red'];

  $profile->setIsHorizontal(1);
  $profile->setColors($colors);
}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Expression::DS_7349a4c6a5;
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#6495ED', '#E9967A', '#2F4F4F' ];
  my $legend = ['Wild Type', 'sir2A', 'sir2B'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 3});

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

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($rma, $percentile);

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
