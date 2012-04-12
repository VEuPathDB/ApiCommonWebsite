package ApiCommonWebsite::View::GraphPackage::BarPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlotPart );
use ApiCommonWebsite::View::GraphPackage::PlotPart;
use ApiCommonWebsite::View::GraphPackage::Util;

#--------------------------------------------------------------------------------

sub getIsStacked               { $_[0]->{'_stack_bars'                     }}
sub setIsStacked               { $_[0]->{'_stack_bars'                     } = $_[1]}

sub getForceHorizontalXAxis      { $_[0]->{'_force_x_horizontal'             }}
sub setForceHorizontalXAxis      { $_[0]->{'_force_x_horizontal'             } = $_[1]}

sub getIsHorizontal              { $_[0]->{'_is_horizontal'                     }}
sub setIsHorizontal              { $_[0]->{'_is_horizontal'                     } = $_[1]}

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

    print STDERR "PF=$profileFile\n";
    print STDERR "ENF=$elementNamesFile\n";

    $i++;
  }


  my $stDevProfiles = $self->getStDevProfileSetNames;
  $i = 0;
  if (scalar $stDevProfiles> 0) {
    foreach my  $profileSetName (@$stDevProfiles) {

      my $sampleLabels = $profileSampleLabels->[$i];
      unless(ref($sampleLabels) eq 'ARRAY') {
        $sampleLabels = $profileSampleLabels;
      }

      my $suffix = $part . "stderr" . $i;
      my ($stdevFile, $elementNamesFile) = @{$self->writeProfileFiles($profileSetName, $suffix)};
      push(@stdevFiles, $stdevFile);

      print STDERR "STDEV=$stdevFile\n";
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
  my $isHorizontal = $self->getIsHorizontal();

  my $horizontalXAxisLabels = $self->getForceHorizontalXAxis();
  my $yAxisFoldInductionFromM = $self->getMakeYAxisFoldInduction();

  $rAdjustProfile = $rAdjustProfile ? $rAdjustProfile : "";

  $horizontalXAxisLabels = defined($horizontalXAxisLabels) ? 'TRUE' : 'FALSE';

  $yAxisFoldInductionFromM = defined($yAxisFoldInductionFromM) ? 'TRUE' : 'FALSE';

  my $beside = $isStack ? 'FALSE' : 'TRUE';
  my $horiz = $isHorizontal ? 'TRUE' : 'FALSE';

  my $bottomMargin = $self->getElementNameMarginSize();

  my $rv = "
# ---------------------------- BAR PLOT ----------------------------

$profileFiles
$elementNamesFiles
$stdevFiles
$colorsString


screen(screens[screen.i]);
screen.i <- screen.i + 1;

#-------------------------------------------------------------------

if(length(profile.files) != length(element.names.files)) {
  stop(\"profile.files length not equal to element.names.files length\");
}

y.min = $yMin;
y.max = $yMax;

# Create Data Frames to collect values to be plotted
profile.df = as.data.frame(matrix(nrow=length(profile.files)));
profile.df\$V1 = NULL;

stdev.df = as.data.frame(matrix(nrow=length(profile.files)));
stdev.df\$V1 = NULL;


for(i in 1:length(profile.files)) {
  profile.tmp = read.table(profile.files[i], header=T, sep=\"\\t\");

  if(!is.null(profile.tmp\$ELEMENT_ORDER)) {
    profile.tmp = aggregate(profile.tmp, list(profile.tmp\$ELEMENT_ORDER), mean, na.rm=T)
  }
  profile = profile.tmp\$VALUE;

  element.names.df = read.table(element.names.files[i], header=T, sep=\"\\t\");
  element.names = as.character(element.names.df\$NAME);

   if(!is.null(stdev.files)) {
     stdev.tmp = read.table(stdev.files[i], header=T, sep=\"\\t\");

     if(!is.null(stdev.tmp\$ELEMENT_ORDER)) {
       stdev.tmp = aggregate(stdev.tmp, list(stdev.tmp\$ELEMENT_ORDER), mean, na.rm=T)
     }
    stdev = stdev.tmp\$VALUE;
   }

  for(j in 1:length(element.names)) {
    this.name = element.names[j];

    if(is.null(.subset2(profile.df, this.name, exact=TRUE))) {
      profile.df[[this.name]] = NA;
    }


    profile.df[[this.name]][i] = profile[j];

    if(!is.null(stdev.files)) {
      if(is.null(.subset2(stdev.df, this.name, exact=TRUE))) {
       stdev.df[[this.name]] = NA;
      }

      stdev.df[[this.name]][i] = stdev[j];
    }
  }
}

# allow minor adjustments to profile
$rAdjustProfile

names.margin = $bottomMargin;
fold.induction.margin = 1;
if($yAxisFoldInductionFromM) {
  fold.induction.margin = 2.5;
}


# stdev.df will either be the same dim as profile.df or 0
if(length(stdev.df) == 0) {
  stdev.df = 0;
}

if($beside) {
  d.max = max(1.1 * profile.df, 1.1 * (profile.df + stdev.df), y.max, na.rm=TRUE);
  d.min = min(1.1 * profile.df, 1.1 * (profile.df - stdev.df), y.min, na.rm=TRUE);
  my.space=c(0,.2);
} else {
  d.max = max(1.1 * profile.df, 1.1 * apply(profile.df, 2, sum), y.max, na.rm=TRUE);
  d.min = min(1.1 * profile.df, 1.1 * apply(profile.df, 2, sum), y.min, na.rm=TRUE);
  my.space = 2;
}

my.las = 2;
if(max(nchar(element.names)) < 4 || $horizontalXAxisLabels) {
  my.las = 0;
}

# c(bottom,left,top,right)


if($horiz) {
  par(mar       = c(5,names.margin,fold.induction.margin,2), xpd=FALSE, oma=c(1,1,1,1));
  x.lim = c(d.min, d.max);
  y.lim = NULL;

  yaxis.side = 1;
  foldchange.side = 3;

  yaxis.line = 2;

} else {
  par(mar       = c(names.margin,4,2,fold.induction.margin), xpd=FALSE, oma=c(1,1,1,1));
  y.lim = c(d.min, d.max);
  x.lim = NULL;

  yaxis.side = 2;
  foldchange.side = 4;

  yaxis.line = 3.5;
}

  plotXPos = barplot(as.matrix(profile.df),
             col       = the.colors,
             xlim      = x.lim,
             ylim      = y.lim,
             beside    = $beside,
             names.arg = colnames(profile.df),
             space = my.space,
             las = my.las,
             axes = FALSE,
             cex.names = 0.9,
             axis.lty  = \"solid\",
             horiz=$horiz
            );

mtext('$yAxisLabel', side=yaxis.side, line=yaxis.line, cex.lab=1, las=0)
yAxis = axis(foldchange.side, tick=F, labels=F);

if($yAxisFoldInductionFromM) {
  foldchange.labels = vector();

  for(i in 1:length(yAxis)) {
    value = yAxis[i];
    if(value > 0) {
      foldchange.labels[i] = round(2^value, digits=1)
    }
    if(value < 0) {
      foldchange.labels[i] = round(-1 * (1 / (2^value)), digits=1);
    }
    if(value == 0) {
      foldchange.labels[i] = 0;
    }
  }

  mtext('Fold Change', side=foldchange.side, line=2, cex.lab=1, las=0)
  axis(foldchange.side,at=yAxis,labels=foldchange.labels,tick=T);  
  axis(yaxis.side,tick=T,labels=T);
} else {
  axis(yaxis.side);
}

lowerBound = as.matrix(profile.df - stdev.df);
upperBound = as.matrix(profile.df + stdev.df);

if($horiz) {
  lines (c(0,0), c(0,length(profile.df) * 2), col=\"gray25\");

  suppressWarnings(arrows(lowerBound,  plotXPos, upperBound, plotXPos, angle=90, code=3, length=0.05, lw=2));
} else {
  lines (c(0,length(profile.df) * 2), c(0,0), col=\"gray25\");

  for(i in 1:nrow(profile.df)) {
    for(j in 1:ncol(profile.df)) {
      if(is.na(profile.df[i,j])) {
        x_coord = plotXPos[i,j];
        y_coord = (d.min + d.max) / 2;
        points(x_coord, y_coord, cex=2, col=\"red\", pch=8);
      }
    }
  }
  suppressWarnings(arrows(plotXPos, lowerBound,  plotXPos, upperBound, angle=90, code=3, length=0.05, lw=2));
}

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
   $self->setAdjustProfile('profile.df=profile.df + 1; profile.df = log2(profile);');

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


