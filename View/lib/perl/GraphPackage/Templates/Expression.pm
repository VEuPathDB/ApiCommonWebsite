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
sub finalPercentileAdjustments {}

# Template subclasses need to implement this....should return 'bar' or 'line'
sub getGraphType {}

sub getPercentileGraphType {
  my $self = shift;
  return $self->getGraphType();
}

sub sortPercentileProfiles {
  $a cmp $b;
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

  my $allProfileSetNames = $self->getAllProfileSetNames();

  my $profileSetsArray = $self->getProfileSetsArray($allProfileSetNames);
  my $percentileSetsArray = $self->getPercentileSetsArray($allProfileSetNames);

  $self->makeAndSetPlots($profileSetsArray, $percentileSetsArray);

  return $self;
}

sub getAllProfileSetNames {
  my ($self) = @_;

  my $datasetName = $self->getDataset();

  my $id = $self->getId();

  my $dbh = $self->getQueryHandle();

  my $sql = ApiCommonWebsite::View::GraphPackage::Util::getProfileSetsSql();

  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetName);

  my @rv = ();

  while(my ($profileName) = $sh->fetchrow_array()) {
    next if($self->isExcludedProfileSet($profileName));
    push @rv, $profileName;
  }
  $sh->finish();


  if ($sh->rows() == 0 ){
    my $filterSql =  ApiCommonWebsite::View::GraphPackage::Util::getFilteredProfileSetsSql();
    $sh = $dbh->prepare($filterSql);
    $sh->execute($datasetName,$id);

    while(my ($profileName) = $sh->fetchrow_array()) {
      next if($self->isExcludedProfileSet($profileName));
      push @rv, $profileName;
    }
    $sh->finish();
  }
  
  return \@rv;
}


sub getProfileSetsArray {
  my ($self, $allProfileSetNames) = @_;

  my (%stderrProfiles, @profiles);

  foreach my $profileName (@$allProfileSetNames) {
    next if($profileName =~ /percentile/i);

    if($profileName =~ /^standard error - /) {
      $stderrProfiles{$profileName} = 1;
    } 
    else {
      push @profiles, $profileName;
    }
  }

  my @profileSetsArray;

  foreach my $profile (sort @profiles) {
    my $expectedStderrProfileName = "standard error - $profile";

    my $stderrProfile = $stderrProfiles{$expectedStderrProfileName} ? $expectedStderrProfileName : "";

    push @profileSetsArray, [$profile, $stderrProfile];
  }
  return \@profileSetsArray;
}

sub getPercentileSetsArray {
  my ($self, $allProfileSetNames) = @_;

  my @percentiles;
  foreach my $profileName (@$allProfileSetNames) {
    next unless($profileName =~ /percentile/i);
    push @percentiles, $profileName;
  }

  my @sortedPercentiles = sort sortPercentileProfiles @percentiles;

  my @percentileSetsArray;
  foreach my $pctProfile (@sortedPercentiles) {
    push @percentileSetsArray, [$pctProfile, ''];
  }

  return \@percentileSetsArray;
}

sub makeAndSetPlots {
  my ($self, $profileSetsArray, $percentileSetsArray) = @_;

  my $bottomMarginSize = $self->getBottomMarginSize();
  my $colors= $self->getColors();
  my $pctColors= $self->getPercentileColors();
  my $sampleLabels = $self->getSampleLabels();

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets($profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets($percentileSetsArray);

  my $profile;
  my $xAxisLabel;

  my $plotPartModule = $self->getExprPlotPartModuleString();  
  if(lc($self->getGraphType()) eq 'bar') {
    my $plotObj = "ApiCommonWebsite::View::GraphPackage::BarPlot::$plotPartModule";

    $profile = eval {
      $plotObj->new(@_);
    };


    if ($@) {
      die "Unable to make plot $plotObj: $@";
    }

    $profile->setForceHorizontalXAxis($self->forceXLabelsHorizontal());

  } elsif(lc($self->getGraphType()) eq 'line') {
    my $plotObj = "ApiCommonWebsite::View::GraphPackage::LinePlot::$plotPartModule";
    $xAxisLabel= $self->getXAxisLabel();

    $profile = eval {
      $plotObj->new(@_);
    };

    if ($@) {
      die "Unable to make plot $plotObj: $@";
    }


  } else {
    die "Graph must define a graph type of bar or line";
  }

  $profile->setProfileSets($profileSets);
  $profile->setColors($colors);
  $profile->setAdjustProfile($self->getProfileRAdjust());

  my $percentile;
  if(lc($self->getPercentileGraphType()) eq 'line') {
    $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
    $percentile->setXaxisLabel($xAxisLabel);
  }
  else {
    $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
    $percentile->setForceHorizontalXAxis($self->forceXLabelsHorizontal());
  }

  $percentile->setProfileSets($percentileSets);

  $percentile->setColors($pctColors);
  $percentile->setAdjustProfile($self->getPercentileRAdjust());

  if($bottomMarginSize) {
    $profile->setElementNameMarginSize($bottomMarginSize);
    $percentile->setElementNameMarginSize($bottomMarginSize);
  }
  
  if($xAxisLabel) {
    $profile->setXaxisLabel($xAxisLabel);
  }

  if(@$sampleLabels) {
    $profile->setSampleLabels($sampleLabels);
    $percentile->setSampleLabels($sampleLabels);
  }

  # These can be implemented by the subclass if needed
  $self->finalProfileAdjustments($profile);
  $self->finalPercentileAdjustments($percentile);

  $self->setGraphObjects($profile, $percentile);
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

#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR microarrayMRNADecayGraph

package ApiCommonWebsite::View::GraphPackage::Templates::Expression::pfal3D7_microarrayExpression_Llinas_RT_Transcription_Decay_RSRC;

use Data::Dumper;
sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  
  my $legendLabels = (['labeled','total','total fitted','unlabeled']);
  $profile->setPointsPch([ 'NA', 'NA', 'NA', 'NA']);
  $profile->setHasExtraLegend(1);
  $profile->setLegendLabels($legendLabels);
}

sub finalPercentileAdjustments {
  my ($self, $percentile) = @_;

  $percentile->setPointsPch([ 'NA', 'NA', 'NA', 'NA']);
}

sub setGraphObjects { 
  my $self = shift;
  my $graphs = [];
  
  my $legendLabels = (['labeled','total','total fitted','unlabeled']);
  foreach my $plotPart (@_) {
    my $name = $plotPart->setHasExtraLegend(1);
    my $size = $plotPart->setLegendLabels($legendLabels);


    push @{$graphs}, $plotPart;
  }

  my $pch = ['NA','NA'];
  my $colors = ['grey','black'];
  my $legend = ['Total Expression', 'Total Expression - smoothed'];

  
  my @profileArray = (
                      ['Llinas RT transcription and decay total Profiles - loess'],
                      ['Llinas RT transcription and decay total Profiles - loess - smoothed']
                     );


  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  print STDERR Dumper $profilesSets;
 
  my $line = ApiCommonWebsite::View::GraphPackage::LinePlot->new(@_);
  $line->setProfileSets([$profileSets->[0],$profileSets->[1]]);
  $line->setPartName('exprn_val_log_ratio');
  $line->setYaxisLabel('Expression Values (log2 ratio)');
  $line->setPointsPch($pch);
  $line->setColors([$colors->[0], $colors->[1]]);
  $line->setArePointsLast(1);
  $line->setElementNameMarginSize(6);
  $line->setXaxisLabel('Hours post infection');
  $line->setHasExtraLegend(1);
  $line->setLegendLabels(['total', 'smoothed']);
  $line->setXaxisLabel('Hours post infection');
  my $id = $self->getId();
  $line->setPlotTitle("Expression Values - $id - Total Expression");
  push (@{$graphs},$line);
  $self->SUPER::setGraphObjects(@{$graphs});
}

1;
