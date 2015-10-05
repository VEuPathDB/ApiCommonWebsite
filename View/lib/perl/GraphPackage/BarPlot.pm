package ApiCommonWebsite::View::GraphPackage::BarPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlotPart );
use ApiCommonWebsite::View::GraphPackage::PlotPart;
use ApiCommonWebsite::View::GraphPackage::Util;
use ApiCommonWebsite::View::GraphPackage;

#--------------------------------------------------------------------------------

sub getIsStacked                 { $_[0]->{'_stack_bars'                     }}
sub setIsStacked                 { $_[0]->{'_stack_bars'                     } = $_[1]}

sub getForceHorizontalXAxis      { $_[0]->{'_force_x_horizontal'             }}
sub setForceHorizontalXAxis      { $_[0]->{'_force_x_horizontal'             } = $_[1]}

sub getIsHorizontal              { $_[0]->{'_is_horizontal'                  }}
sub setIsHorizontal              { $_[0]->{'_is_horizontal'                  } = $_[1]}

sub getHighlightMissingValues    { $_[0]->{'_highlight_missing_values'       }}
sub setHighlightMissingValues    { $_[0]->{'_highlight_missing_values'       } = $_[1]}

sub getSpaceBetweenBars          { $_[0]->{'_space_between_bars'             }}
sub setSpaceBetweenBars          { $_[0]->{'_space_between_bars'             } = $_[1]}

sub getAxisPadding          { $_[0]->{'_axis_padding'             }}
sub setAxisPadding          { $_[0]->{'_axis_padding'             } = $_[1]}


#--------------------------------------------------------------------------------

sub new {
  my $class = shift;

   my $self = $class->SUPER::new(@_);

   $self->setSpaceBetweenBars(0.1);
  $self->setAxisPadding(1.1);
   return $self;
}

#--------------------------------------------------------------------------------
sub makeRPlotString {
  my ($self, $idType) = @_;

  my $sampleLabels = $self->getSampleLabels();
  my $sampleLabelsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($sampleLabels, 'x.axis.label');
  my $overrideXAxisLabels = scalar @$sampleLabels > 0 ? "TRUE" : "FALSE";


  my ($profileFiles, $elementNamesFiles, $stderrFiles);

  eval{
   ($profileFiles, $elementNamesFiles, $stderrFiles) = $self->makeFilesForR($idType);
 };

  if($@) {
    return $self->blankPlotPart();
  }

  foreach(@{$self->getProfileSets()}) {
    if(scalar @{$_->errors()} > 0) {
      return $self->blankPlotPart();
    }
  }

  my $colors = $self->getColors();
  my $colorsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($colors, 'the.colors');

  my $rAdjustProfile = $self->getAdjustProfile();
  my $yAxisLabel = $self->getYaxisLabel();
  my $plotTitle = $self->getPlotTitle();

  my $yMax = $self->getDefaultYMax();
  my $yMin = $self->getDefaultYMin();


  my $isCompactString = "FALSE";

  if($self->isCompact()) {
    $isCompactString = "TRUE";
  }

  my $isStack = $self->getIsStacked();
  my $isHorizontal = $self->getIsHorizontal();

  my $horizontalXAxisLabels = $self->getForceHorizontalXAxis();
  my $yAxisFoldInductionFromM = $self->getMakeYAxisFoldInduction();
  my $highlightMissingValues = $self->getHighlightMissingValues();

  $highlightMissingValues = $highlightMissingValues ? 'TRUE' : 'FALSE';

  $rAdjustProfile = $rAdjustProfile ? $rAdjustProfile : "";

  $horizontalXAxisLabels = $horizontalXAxisLabels ? 'TRUE' : 'FALSE';

  $yAxisFoldInductionFromM = $yAxisFoldInductionFromM ? 'TRUE' : 'FALSE';

  my $beside = $isStack ? 'FALSE' : 'TRUE';
  my $horiz = $isHorizontal && !$self->isCompact() ? 'TRUE' : 'FALSE';

  my $titleLine = $self->getTitleLine();

  my $bottomMargin = $self->getElementNameMarginSize();
  my $spaceBetweenBars = $self->getSpaceBetweenBars();

  my $scale = $self->getScalingFactor;

  my $hasExtraLegend = $self->getHasExtraLegend() ? 'TRUE' : 'FALSE';
  my $extraLegendSize = $self->getExtraLegendSize();

  my $axisPadding = $self->getAxisPadding();

  my $rv = "
# ---------------------------- BAR PLOT ----------------------------

$profileFiles
$elementNamesFiles
$stderrFiles
$colorsString
$sampleLabelsString

is.compact=$isCompactString;

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

stderr.df = as.data.frame(matrix(nrow=length(profile.files)));
stderr.df\$V1 = NULL;


for(i in 1:length(profile.files)) {
  profile.tmp = read.table(profile.files[i], header=T, sep=\"\\t\");

  profile = profile.tmp\$VALUE;

  element.names.df = read.table(element.names.files[i], header=T, sep=\"\\t\");
  element.names = as.character(element.names.df\$NAME);

   if(!is.na(stderr.files[i]) && stderr.files[i] != '') {
     stderr.tmp = read.table(stderr.files[i], header=T, sep=\"\\t\");

    stderr = stderr.tmp\$VALUE;
   } else {
     stderr = element.names;
     stderr = NA;
   }


  for(j in 1:length(element.names)) {
    this.name = element.names[j];

    if(is.null(.subset2(profile.df, this.name, exact=TRUE))) {
      profile.df[[this.name]] = NA;
    }


    profile.df[[this.name]][i] = profile[j];

    if(is.null(.subset2(stderr.df, this.name, exact=TRUE))) {
     stderr.df[[this.name]] = NA;
    }

    stderr.df[[this.name]][i] = stderr[j];
  }
}

# allow minor adjustments to profile
$rAdjustProfile

names.margin = $bottomMargin;
fold.induction.margin = 1;
if($yAxisFoldInductionFromM) {
  fold.induction.margin = 3.5;
}


# stderr.df will either be the same dim as profile.df or 0
if(length(stderr.df) == 0) {
  stderr.df = 0;
}


my.space = $spaceBetweenBars;

if($beside) {
  d.max = max($axisPadding * profile.df, $axisPadding * (profile.df + stderr.df), y.max, na.rm=TRUE);
  d.min = min($axisPadding * profile.df, $axisPadding * (profile.df - stderr.df), y.min, na.rm=TRUE);
  my.space=c(0, my.space);
} else {
  d.max = max($axisPadding * profile.df, $axisPadding * apply(profile.df, 2, sum), y.max, na.rm=TRUE);
  d.min = min($axisPadding * profile.df, $axisPadding * apply(profile.df, 2, sum), y.min, na.rm=TRUE);
  my.space = my.space;
}

# set left margin size based on longest tick mark label

long.tick = nchar(max(pretty(c(d.min,d.max))));

left.margin.size = 4;

if((long.tick +1 ) > left.margin.size) {
    left.margin.size = ((long.tick)+1);
}

# c(bottom,left,top,right)

# extra legend size specified in # of lines

extra.legend.size = 0;
if($hasExtraLegend) {
  extra.legend.size = $extraLegendSize;
}


title.line = $titleLine;

if(is.compact) {
  par(mar       = c(0,0,0,0),xaxt=\"n\", bty=\"n\", xpd=TRUE);
}

if($horiz) {
  par(mar       = c(5, names.margin,title.line + fold.induction.margin, 1 + extra.legend.size), xpd=NA, oma=c(1,1,1,1));
  x.lim = c(d.min, d.max);
  y.lim = NULL;

  yaxis.side = 1;
  foldchange.side = 3;

  yaxis.line = 2;

} else {

  if(!is.compact) {
    par(mar       = c(names.margin,left.margin.size,1.5 + title.line,fold.induction.margin + extra.legend.size), xpd=NA);
  } 
  y.lim = c(d.min, d.max);
  x.lim = NULL;

  yaxis.side = 2;
  foldchange.side = 4;

  yaxis.line = left.margin.size - 1;
}

if($overrideXAxisLabels) {
  my.labels = x.axis.label;
} else {
  my.labels = colnames(profile.df);
}

my.las = 0;
if(max(nchar(my.labels)) > 4 && !($horizontalXAxisLabels)) {
  my.las = 2;
}


  plotXPos = barplot(as.matrix(profile.df),
             col       = the.colors,
             xlim      = x.lim,
             ylim      = y.lim,
             beside    = $beside,
             names.arg = my.labels,
             space = my.space,
             las = my.las,
             axes = FALSE,
             cex.axis=$scale,
             axis.lty  =  \"solid\",
             horiz=$horiz,
            );

mtext('$yAxisLabel', side=yaxis.side, line=yaxis.line, cex=$scale, las=0)
yAxis = axis(foldchange.side, tick=F, labels=F);


if($yAxisFoldInductionFromM && !is.compact) {
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

  mtext('Fold Change', side=foldchange.side, line=2, cex=$scale, las=0)
  axis(foldchange.side,at=yAxis,labels=foldchange.labels,tick=T);  
  axis(yaxis.side,tick=T,labels=T);
} else {
  axis(yaxis.side);
}

lowerBound = as.matrix(profile.df - stderr.df);
upperBound = as.matrix(profile.df + stderr.df);


plotLength = max(plotXPos) + min(plotXPos);


if(!is.compact) {
if($horiz) {
#  lines (c(0,0), c(0,length(profile.df) * 2), col=\"gray25\");
  lines (c(0,0), c(0,plotLength), col=\"gray25\");

  suppressWarnings(arrows(lowerBound,  plotXPos, upperBound, plotXPos, angle=90, code=3, length=0.05, lw=2));
} else {
#  lines (c(0,length(profile.df) * 2), c(0,0), col=\"gray25\");
  lines (c(0,plotLength), c(0,0), col=\"gray25\");



  if($highlightMissingValues) {
    for(i in 1:nrow(profile.df)) {
      for(j in 1:ncol(profile.df)) {
        if(is.na(profile.df[i,j])) {
          x_coord = plotXPos[i,j];
          y_coord = (d.min + d.max) / 2;
          points(x_coord, y_coord, cex=2, col=\"red\", pch=8);
        }
      }
    }
  }
  suppressWarnings(arrows(plotXPos, lowerBound,  plotXPos, upperBound, angle=90, code=3, length=0.05, lw=2));

}
}

box();


if($hasExtraLegend && !is.compact) {
  # To add a legend into the margin... you need to convert ndc coordinates into user coordinates
  figureRegionXMax = par()\$fig[2];
  figureRegionYMax = par()\$fig[4];

  legend(grconvertX(figureRegionXMax, from='ndc', to='user'),
         grconvertY(figureRegionYMax, from='ndc', to='user'),
         my.labels,
         cex   = (0.8 * $scale),
         ncol  = 1,
         fill=the.colors,
         bty='n',
         xjust=1,
         yjust=1
        );
}

  plasmodb.title(\"$plotTitle\", line=title.line);
";

  return $rv;
}

1;

package ApiCommonWebsite::View::GraphPackage::BarPlot::RMA;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift;
  my $self = $class->SUPER::new(@_);

  my $id = $self->getId();
  my $wantLogged = $self->getWantLogged();

  $self->setYaxisLabel("RMA Value (log2)");
  $self->setIsLogged(1);

  # RMAExpress is log2
  if(defined($wantLogged) && $wantLogged eq '0') {
    $self->setAdjustProfile('profile.df = 2^(profile.df);stderr.df = 2^stderr.df;');
    $self->setYaxisLabel("RMA Value");
  }

  $self->setDefaultYMax(4);
  $self->setDefaultYMin(0);

  $self->setPartName('rma');
  $self->setPlotTitle("RMA Expression Value - $id");

  return $self;
}

1;

package ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();
   $self->setPartName('percentile');
   $self->setYaxisLabel('Percentile');
   $self->setDefaultYMax(100);
   $self->setDefaultYMin(0);
   $self->setIsLogged(0);

   $self->setPlotTitle("Percentile - $id");
   return $self;
}
1;



package ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStackedSpliced;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
  my $self = $class->SUPER::new(@_);

  my $id = $self->getId();

  $self->setPartName('rpm');
  $self->setYaxisLabel('RPM');
  $self->setIsStacked(1);
  $self->setDefaultYMin(0);
  $self->setDefaultYMax(50);
  $self->setPlotTitle("RPM - $id");

  return $self;
}




package ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
  my $self = $class->SUPER::new(@_);

  my $id = $self->getId();
  my $wantLogged = $self->getWantLogged();

  $self->setPartName('rpkm');
  $self->setYaxisLabel('RPKM');
  $self->setIsStacked(1);
  $self->setDefaultYMin(0);
  $self->setDefaultYMax(50);
  $self->setPlotTitle("RPKM - $id");

  # RUM RPKM Are Not logged in the db
  # JB:  Cannot take the log2 of the diff profiles then add
#  if($wantLogged) {
#    $self->setAdjustProfile('profile.df=profile.df + 1; profile.df = log2(profile.df);');
#    $self->setYaxisLabel('RPKM (log2)');
#    $self->setIsLogged(1);
#    $self->setDefaultYMax(4);
#  }

  return $self;
}

package ApiCommonWebsite::View::GraphPackage::BarPlot::PairedEndRNASeqStacked;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked);
use strict;

sub new {
  my $class = shift; 
  my $self = $class->SUPER::new(@_);

  my $id = $self->getId();

  $self->setPartName('fpkm');
  $self->setYaxisLabel('FPKM');
  $self->setPlotTitle("FPKM - $id");

  # RUM RPKM Are Not logged in the db
  # JB:  Cannot take the log2 of the diff profiles then add
#  if($wantLogged) {
#    $self->setAdjustProfile('profile.df=profile.df + 1; profile.df = log2(profile.df);');
#    $self->setYaxisLabel('RPKM (log2)');
#    $self->setIsLogged(1);
#    $self->setDefaultYMax(4);
#  }

  return $self;
}

#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(1);
   $self->setDefaultYMin(-1);
   $self->setYaxisLabel('Expression Value (log2 ratio)');

   $self->setPartName('exprn_val');
   $self->setPlotTitle("Log(ratio) - $id");

   $self->setMakeYAxisFoldInduction(1);
   $self->setIsLogged(1);

   return $self;
}

package ApiCommonWebsite::View::GraphPackage::BarPlot::QuantileNormalized;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(4);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('Expression Value (log2)');

   $self->setPartName('exprn_val');
   $self->setPlotTitle("Expression Values (log2) - $id");

   $self->setMakeYAxisFoldInduction(0);
   $self->setIsLogged(1);

   return $self;
}

package ApiCommonWebsite::View::GraphPackage::BarPlot::MRNADecay;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(4);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('Expression Value');

   $self->setPartName('exprn_val');
   $self->setPlotTitle("Expression Values - $id");

   $self->setMakeYAxisFoldInduction(0);
   $self->setIsLogged(0);

   return $self;
}


package ApiCommonWebsite::View::GraphPackage::BarPlot::Standardized;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(1);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('Median Expr (standardized)');

   $self->setPartName('exprn_val');
   $self->setPlotTitle("Median Expr (standardized) - $id");

   $self->setMakeYAxisFoldInduction(0);
   $self->setIsLogged(1);

   return $self;
}

package ApiCommonWebsite::View::GraphPackage::BarPlot::MassSpec;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(10);
   $self->setDefaultYMin(0);
#   $self->setYaxisLabel('Mass');
   $self->setYaxisLabel('');

   $self->setPartName('mass_spec');
   $self->setPlotTitle("Mass Profile - $id");

   $self->setMakeYAxisFoldInduction(0);
   $self->setIsLogged(0);

   return $self;
}

package ApiCommonWebsite::View::GraphPackage::BarPlot::QuantMassSpec;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setIsLogged(1);

   $self->setDefaultYMax(1);
   $self->setDefaultYMin(-1);
   $self->setYaxisLabel('Relative Abundance (log2 ratio)');

   $self->setPartName('exprn_val');
   $self->setPlotTitle("Quant Mass Spec Profile - $id");

   $self->setMakeYAxisFoldInduction(1);


   return $self;
}

package ApiCommonWebsite::View::GraphPackage::BarPlot::QuantMassSpecNonRatio;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(4);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('Abundance (log2)');

   $self->setPartName('exprn_val');
   $self->setPlotTitle("Quant Mass Spec Profile - $id");

   return $self;
}

package ApiCommonWebsite::View::GraphPackage::BarPlot::QuantMassSpecNonRatioUnlogged;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(20);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('Abundance');

   $self->setPartName('exprn_val');
   $self->setPlotTitle("Quant Mass Spec Profile - $id");

   return $self;
}



package ApiCommonWebsite::View::GraphPackage::BarPlot::SageTag;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(0.01);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('Percents');

   $self->setPartName('sage_tags');
   $self->setPlotTitle("Sage Tag Profile - $id");

   $self->setMakeYAxisFoldInduction(0);
   $self->setIsLogged(0);

   return $self;
}



package ApiCommonWebsite::View::GraphPackage::BarPlot::Genera;
use base qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(0.2);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('');

   $self->setPartName('sage_tags');
   $self->setPlotTitle("Genera - $id");

   $self->setMakeYAxisFoldInduction(0);
   $self->setIsLogged(0);

  $self->setAxisPadding(1);
   $self->setSpaceBetweenBars(0);
   return $self;
}


1;


