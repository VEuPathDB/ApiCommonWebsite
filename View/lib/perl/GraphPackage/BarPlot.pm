package ApiCommonWebsite::View::GraphPackage::BarPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlotPart );
use ApiCommonWebsite::View::GraphPackage::PlotPart;
use ApiCommonWebsite::View::GraphPackage::Util;

use Data::Dumper;
#--------------------------------------------------------------------------------

sub getIsStacked               { $_[0]->{'_stack_bars'                     }}
sub setIsStacked               { $_[0]->{'_stack_bars'                     } = $_[1]}

sub getForceHorizontalXAxis      { $_[0]->{'_force_x_horizontal'             }}
sub setForceHorizontalXAxis      { $_[0]->{'_force_x_horizontal'             } = $_[1]}

#--------------------------------------------------------------------------------

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);
   $self->SUPER::init;
   return $self;
}

#--------------------------------------------------------------------------------
sub makeRPlotString {
  my ($self) = @_;

  my $part = $self->getPartName();
  
  my (@profileFiles, @elementNamesFiles, @stdevFiles);
  my $i = 0;
  my ($pf, $enf);
  # each part can have several profile sets
  my $profiles = $self->getProfileSetNames;

  my $profileSampleLabels = $self->getSampleLabels();

  foreach my $profileSetName (@$profiles) {

    my $sampleLabels = $profileSampleLabels->[$i];
    unless(ref($sampleLabels) eq 'ARRAY') {
      $sampleLabels = $profileSampleLabels;
    }

    my $suffix = $part . $i;
    my ($profileFile, $elementNamesFile) = @{$self->writeProfileFiles($profileSetName, $suffix)};

    if($profileFile && $elementNamesFile) {
      push(@profileFiles, $profileFile);
      push(@elementNamesFiles, $elementNamesFile);

    }
    $i++;
  }

  my $stDevProfiles = $self->getStDevProfileSetNames;
  if (scalar $stDevProfiles> 0) {
    foreach my  $profileSetName (@$stDevProfiles) {

      my $sampleLabels = $profileSampleLabels->[$i];
      unless(ref($sampleLabels) eq 'ARRAY') {
        $sampleLabels = $profileSampleLabels;
      }

      my $suffix = $part . $i;
      my ($stdevFile, $elementNamesFile) = @{$self->writeProfileFiles($profileSetName, $suffix)};
      push(@stdevFiles, $stdevFile);
      $i++;
    }
  }
  die "no profile files" unless(scalar @profileFiles > 0);
  my $profileFilesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@profileFiles, 'profile.files');
  my $elementNamesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@elementNamesFiles, 'element.names.files');
  my $stdevString ='';
  if (scalar $stDevProfiles> 0) {
    $stdevString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@stdevFiles, 'stdev.files');
  }

  my $colors = $self->getColors();

  my $rColorsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($colors, 'the.colors');


#  TODO: Determine if this is a property of the PlotSet or the PlotPart. May need to set from MixedPlotSet
#  my $legend = $profileSetsHash->{$part}->{legend};
#  my $rLegendString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($legend, 'the.legend');


  
  my $rCode = $self->rString($profileFilesString, $elementNamesString, $stdevString, $rColorsString);

  return $rCode;
}

#--------------------------------------------------------------------------------

sub rString {
  my ($self, $profileFiles, $elementNamesFiles, $stdevFiles, $colorsString ) = @_;

  my $rAdjustProfile = $self->getAdjustProfile();
  my $yAxisLabel = $self->getYaxisLabel();
  my $plotTitle = $self->getPlotTitle();

  my $yMax = $self->getDefaultYMax();
  my $yMin = $self->getDefaultYMin();

  my $isStack = $self->getIsStacked();

  my $horizontalXAxisLabels = $self->getForceHorizontalXAxis();
  my $yAxisFoldInductionFromM = $self->getMakeYAxisFoldInduction();

  $rAdjustProfile = $rAdjustProfile ? $rAdjustProfile : "";

  $horizontalXAxisLabels = defined($horizontalXAxisLabels) ? 'TRUE' : 'FALSE';

  $yAxisFoldInductionFromM = defined($yAxisFoldInductionFromM) ? 'TRUE' : 'FALSE';

  my $beside = defined($isStack) ? 'FALSE' : 'TRUE';

  my $bottomMargin = $self->getBottomMarginSize();

  my $rv = "
# ---------------------------- BAR PLOT ----------------------------

$profileFiles
$elementNamesFiles
$stdevFiles
$colorsString

y.min = $yMin;
y.max = $yMax;

screen(screens[screen.i]);
screen.i <- screen.i + 1;

profile = vector();
for(i in 1:length(profile.files)) {
  tmp = read.table(profile.files[i], header=T, sep=\"\\t\");

  if(!is.null(tmp\$ELEMENT_ORDER)) {
    tmp = aggregate(tmp, list(tmp\$ELEMENT_ORDER), mean, na.rm=T)
  }
  profile = rbind(profile, tmp\$VALUE);
}

element.names = vector(\"character\");
for(i in 1:length(element.names.files)) {
  tmp = read.table(element.names.files[i], header=T, sep=\"\\t\");
  element.names = rbind(element.names, as.vector(tmp\$NAME));
}

stdev = vector();
 if(!is.null(stdev.files)) {
   for(i in 1:length(stdev.files)) {
     tmp = read.table(stdev.files[i], header=T, sep=\"\\t\");

     if(!is.null(tmp\$ELEMENT_ORDER)) {
        tmp = aggregate(tmp, list(tmp\$ELEMENT_ORDER), mean, na.rm=T)
      }
     stdev = rbind(stdev, tmp\$VALUE);
   }
 }

if(!length(stdev) > 0) {
  stdev = 0;
}


par(mar       = c($bottomMargin,4,2,4), xpd=FALSE, oma=c(1,1,1,1));

# Allow Subclass to fiddle with the data structure and x axis names
$rAdjustProfile

if($beside) {
  d.max = max(1.1 * profile, 1.1 * (profile + stdev), y.max, na.rm=TRUE);
  d.min = min(1.1 * profile, 1.1 * (profile - stdev), y.min, na.rm=TRUE);
  my.space=c(0,.5);
} else {
  d.max = max(1.1 * profile, 1.1 * apply(profile, 2, sum), y.max, na.rm=TRUE);
  d.min = min(1.1 * profile, 1.1 * apply(profile, 2, sum), y.min, na.rm=TRUE);
  my.space = 0.2;
}

my.las = 2;
if(max(nchar(element.names)) < 6 || $horizontalXAxisLabels) {
  my.las = 0;
}

plotXPos = barplot(profile,
           col       = the.colors,
           ylim      = c(d.min, d.max),
           beside    = $beside,
           names.arg = element.names,
           space = my.space,
           las = my.las,
           axes = FALSE,
           cex.names = 0.8
          );


mtext('$yAxisLabel', side=2, line=3.5, cex.lab=1, las=0)


yAxis = axis(4,tick=F,labels=F);
if($yAxisFoldInductionFromM) {
  yaxis.labels = vector();

  for(i in 1:length(yAxis)) {
    value = yAxis[i];
    if(value > 0) {
      yaxis.labels[i] = round(2^value, digits=1)
    }
    if(value < 0) {
      yaxis.labels[i] = round(-1 * (1 / (2^value)), digits=1);
    }
    if(value == 0) {
      yaxis.labels[i] = 0;
    }
  }


  axis(4,at=yAxis,labels=yaxis.labels,tick=T);  
  axis(2,tick=T,labels=T);
   mtext('Fold Change', side=4, line=2, cex.lab=1, las=0)
} else {
  axis(2);  
}

lines (c(0,length(profile) * 2), c(0,0), col=\"gray25\");

for(i in 1:nrow(profile)) {
  for(j in 1:ncol(profile)) {
    if(is.na(profile[i,j])) {
      x_coord = plotXPos[i,j];
      y_coord = (d.min + d.max) / 2;
      points(x_coord, y_coord, cex=2, col=\"red\", pch=8);
    }
  }
}

lowerBound = profile - stdev;
upperBound = profile + stdev;
suppressWarnings(arrows(plotXPos, lowerBound,  plotXPos, upperBound, angle=90, code=3, length=0.05, lw=2));


box();
plasmodb.title(\"$plotTitle\");

";

  return $rv;
}

1;

package ApiCommonWebsite::View::GraphPackage::BarPlot::RMA;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);

   $self->setDefaultYMax(4);
   $self->setDefaultYMin(0);

   $self->setPartName('rma');
   $self->setYaxisLabel("RMA Value (log2)");

   $self->setIsLogged(1);
   return $self;
}

1;

package ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);
   $self->setPartName('percentile');
   $self->setYaxisLabel('Percentile');
   $self->setDefaultYMax(50);
   $self->setDefaultYMin(0);
   $self->setIsLogged(0);
   return $self;
}
1;

package ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);

   $self->setPartName('coverage');
   $self->setYaxisLabel('Normalized Coverage (log2)');
   $self->setIsStacked(1);
   $self->setIsLogged(1);
   $self->setDefaultYMin(0);
   $self->setDefaultYMax(4);
   $self->setAdjustProfile('profile=profile + 1; profile = log2(profile);');

   return $self;
}

#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);

   $self->setDefaultYMax(2);
   $self->setDefaultYMin(-2);

   $self->setPartName('exprn_val');
   $self->setYaxisLabel("Expression Values");

   $self->setMakeYAxisFoldInduction(1);
   $self->setIsLogged(1);

   return $self;
}

1;


1;


