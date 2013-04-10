package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::TwoChannel;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::Util;

use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

sub getColors {
  return ['blue', 'grey'];
}

sub getBottomMarginSize {}


sub init {
  my $self = shift;
  $self->SUPER::init(@_);

  my $datasetName = $self->getDataset();

  my $dbh = $self->getQueryHandle();

  my $sql = ApiCommonWebsite::View::GraphPackage::Util::getProfileSetsSql();

  my $sh = $dbh->prepare($sql);
  $sh->execute($datasetName);


  my ($profile, $stderrProfile, $redPctProfile, $greenPctProfile, $count);

  while(my ($profileName) = $sh->fetchrow_array()) {
    if($profileName =~ /^standard error - /) {
      $stderrProfile = $profileName;
    } elsif($profileName =~ /^red percentile - /) {
      $redPctProfile = $profileName;
    } elsif($profileName =~ /^gree percentile - /) {
      $greenPctProfile = $profileName;
    } else {
      $profile = $profileName;
    }
    $count++;
  }
  $sh->finish();

  die "Expected 4 profile sets but got $count for $datasetName!!" if($count != 4);


  my @profileSetsArray = (['$profile', '$stderrProfile', ]);
  my @percentileSetsArray = (['$redPctProfile', '',],
                             ['$greenPctProfile', '',]);

  $self->makeAndSetPlots(\@profileSetsArray, \@percentileSetsArray);

  return $self;
}

sub makeAndSetPlots {
  my ($self, $profileSetsArray, $percentileSetsArray) = @_;

  my $bottomMarginSize = $self->getBotomMarginSize();
  my $colors= $self->getColors();

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $ratio;
  
  if(lc($self->getGraphType()) eq 'bar') {
    $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  } elsif(lc($self->getGraphType()) eq 'line') {
    $ratio = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  } else {
    die "Graph must define a graph type of bar or line";
  }

  $ratio->setProfileSets($profileSets);
  $ratio->setColors([$colors->[0]]);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);


  if($bottomMarginSize) {
    $ratio->setElementNameMarginSize($bottomMarginSize);
    $percentile->setElementNameMarginSize($bottomMarginSize);
  }

  $self->setGraphObjects($ratio, $percentile);
}

# subclasses need to implement this....should return 'bar' or 'line'
sub getGraphType {}

# this should be overridden by the subclass if we have loaded extra profilesets which are not to be graphed
sub excludedProfileSetsArray { [] }

sub isExcludedProfileSet {
  my ($self, $psName) = @_;

  foreach(@{$self->excludedProfileSetsArray()}) {
    return 1 if($_ eq $psName);
  }
  return 0;
}

1;

#--------------------------------------------------------------------------------


# TEMPLATE_ANCHOR microarraySimpleTwoChannel
