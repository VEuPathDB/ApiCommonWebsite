package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::TwoChannel;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::Util;

use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::LinePlot;



# Subclasses can adjust the RCode but we won't let the templates do this
sub getPercentileRAdjust {}

sub getRatioRAdjust {}

sub isCustomGraph {}

# Template subclasses need to implement this....should return 'bar' or 'line'
sub getGraphType {}

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

  return $self if($self->isCustomGraph());

  my $datasetName = $self->getDataset();

  my $dbh = $self->getQueryHandle();

  my $sql = ApiCommonWebsite::View::GraphPackage::Util::getProfileSetsSql();

  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetName);


  my ($profile, $stderrProfile, $redPctProfile, $greenPctProfile, $count);

  while(my ($profileName) = $sh->fetchrow_array()) {
    next if($self->isExcludedProfileSet($profileName));
    if($profileName =~ /^standard error - /) {
      $stderrProfile = $profileName;
    } elsif($profileName =~ /^red percentile - /) {
      $redPctProfile = $profileName;
    } elsif($profileName =~ /^green percentile - /) {
      $greenPctProfile = $profileName;
    } else {
      $profile = $profileName;
    }
    $count++;
  }
  $sh->finish();

  die "Expected 4 profile sets but got $count for $datasetName!!" if($count != 4);


  my @profileSetsArray = ([$profile, $stderrProfile, ]);
  my @percentileSetsArray = ([$redPctProfile, '',],
                             [$greenPctProfile, '',]);

  $self->makeAndSetPlots(\@profileSetsArray, \@percentileSetsArray);

  return $self;
}


sub makeAndSetPlots {
  my ($self, $profileSetsArray, $percentileSetsArray) = @_;

  my $bottomMarginSize = $self->getBottomMarginSize();
  my $colors= $self->getColors();
  my $sampleLabels = $self->getSampleLabels();

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets($profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets($percentileSetsArray);

  my $ratio;
  
  if(lc($self->getGraphType()) eq 'bar') {
    $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
    $ratio->setForceHorizontalXAxis($self->forceXLabelsHorizontal());

  } elsif(lc($self->getGraphType()) eq 'line') {
    $ratio = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  } else {
    die "Graph must define a graph type of bar or line";
  }

  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);
  $ratio->setAdjustProfile($self->getRatioRAdjust());
  $ratio->setSpaceBetweenBars(0);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setAdjustProfile($self->getPercentileRAdjust());
  $percentile->setForceHorizontalXAxis($self->forceXLabelsHorizontal());


  if($bottomMarginSize) {
    $ratio->setElementNameMarginSize($bottomMarginSize);
    $percentile->setElementNameMarginSize($bottomMarginSize);
  }

  if(@$sampleLabels) {
    $ratio->setSampleLabels($sampleLabels);
    $percentile->setSampleLabels($sampleLabels);
  }

  $self->setGraphObjects($ratio, $percentile);
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


sub forceXLabelsHorizontal {
  my ($self) = @_;

  if(lc($self->getForceXLabelsHorizontalString()) eq 'true') {
    return 1;
  }
  return 0;
}


1;

#--------------------------------------------------------------------------------


# This is an example of customizing a graph.  The template will provide things like colors (ie. we still inject stuff for it below!!
package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::TwoChannel::tbruTREU927_microarrayExpression_EMEXP2026_DHH1_mutant_pLEW100_24H_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Microarray::TwoChannel );
use strict;

sub isCustomGraph { 1 }

sub init {
  my ($self) = @_;
  $self->SUPER::init(@_);

  my @profileSetsArray = (['DHH1 induced vs. uninduced procyclics - wild type', 'standard error - DHH1 induced vs. uninduced procyclics - wild type', ],
                          ['DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', 'standard error - DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', ],
      );

  my @percentileSetsArray = (['red percentile - DHH1 induced vs. uninduced procyclics - wild type', '', ['TEMP']],
                             ['red percentile - DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', '',, ['TEMP']],
                             ['green percentile - DHH1 induced vs. uninduced procyclics - wild type', '',, ['TEMP']],
                             ['green percentile - DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', '',, ['TEMP']],
      );

  $self->makeAndSetPlots(\@profileSetsArray, \@percentileSetsArray);
}

sub getRatioRAdjust { return 'profile.df = t(as.matrix(colSums(profile.df, na.rm=T))); stderr.df = t(as.matrix(colSums(stderr.df, na.rm=T)))'}
sub getPercentileRAdjust { return 'profile.df = rbind(profile.df[1:2,1], profile.df[3:4,1]);stderr.df = 0;' }

1;

# TEMPLATE_ANCHOR microarraySimpleTwoChannel
