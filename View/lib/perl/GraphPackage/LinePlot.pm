package ApiCommonWebsite::View::GraphPackage::LinePlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlotPart );
use ApiCommonWebsite::View::GraphPackage::PlotPart;
use ApiCommonWebsite::View::GraphPackage::Util;

use Data::Dumper;
#--------------------------------------------------------------------------------

sub getForceNoLines              { $_[0]->{'_force_no_lines'                }}
sub setForceNoLines              { $_[0]->{'_force_no_lines'                } = $_[1]}

sub getVaryGlyphByXAxis          { $_[0]->{'_vary_glyph_by_x_axis'          }}
sub setVaryGlyphByXAxis          { $_[0]->{'_vary_glyph_by_x_axis'          } = $_[1]}

sub getPointsPch                 { $_[0]->{'_points_pch'                    }}
sub setPointsPch                 { $_[0]->{'_points_pch'                    } = $_[1]}

sub getDefaultXMax               { $_[0]->{'_default_x_max'                 }}
sub setDefaultXMax               { $_[0]->{'_default_x_max'                 } = $_[1]}

sub getDefaultXMin               { $_[0]->{'_default_x_min'                 }}
sub setDefaultXMin               { $_[0]->{'_default_x_min'                 } = $_[1]}

sub getXaxisLabel                { $_[0]->{'_x_axis_label'                  }}
sub setXaxisLabel                { $_[0]->{'_x_axis_label'                  } = $_[1]}

sub getArePointsLast             { $_[0]->{'_are_points_last'               }}
sub setArePointsLast             { $_[0]->{'_are_points_last'               } = $_[1]}

sub getRTopMarginTitle           { $_[0]->{'_top_margin_title'              }}
sub setRTopMarginTitle           { $_[0]->{'_top_margin_title'              } = $_[1]}

sub getSmoothLines               { $_[0]->{'_smooth_lines'                  }}
sub setSmoothLines               { $_[0]->{'_smooth_lines'                  } = $_[1]}

sub getSplineApproxN             { $_[0]->{'_spline_approx_n'               }}
sub setSplineApproxN             { $_[0]->{'_spline_approx_n'               } = $_[1]}

#--------------------------------------------------------------------------------

sub new {
   my ($class, $args) = @_;
   my $self = $class->SUPER::new($args);
   $self->SUPER::init;
   $self->setXaxisLabel("Whoops! Object forgot to call setXaxisLabel");
   $self->setPointsPch([15]);
   $self->setDefaultYMax(2);
   $self->setDefaultYMin(-2);
   return $self;
}

#--------------------------------------------------------------------------------
sub makeRPlotString {
  my ($self) = @_;
  my @rv;

  my $part = $self->getPartName();
  
  my (@profileFiles, @elementNamesFiles, @stdevFiles);
  my $i = 0;
  my ($pf, $enf);
  # each part can have several profile sets


  my $profileSampleLabels = $self->getSampleLabels();


  my $profiles = $self->getProfileSetNames;
  foreach my $profileSetName (@$profiles) {

    my $suffix = $part . $i;
    my ($profileFile, $elementNamesFile) = @{$self->writeProfileFiles($profileSetName, $suffix, $profileSampleLabels->[$i])};

    if($profileFile && $elementNamesFile) {
      push(@profileFiles, $profileFile);
      push(@elementNamesFiles, $elementNamesFile);
    }
    $i++;

  }

  my $stDevProfiles = $self->getStDevProfileSetNames;
  if (scalar $stDevProfiles> 0) {
    foreach my  $profileSetName (@$stDevProfiles) {
      my $suffix = $part . $i;
      my ($stdevFile, $elementNamesFile) = @{$self->writeProfileFiles($profileSetName, $suffix, $profileSampleLabels->[$i])};
      push(@stdevFiles, $stdevFile);
      $i++;
    }
  }
  die "No Profile Files" unless(scalar @profileFiles > 0);
  my $profileFilesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@profileFiles, 'profile.files');
  my $elementNamesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@elementNamesFiles, 'element.names.files');
  my $stdevString ='';
  if (scalar $stDevProfiles> 0) {
    $stdevString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@stdevFiles, 'stdev.files');
  }
  my $colors = $self->getColors();
  unless ($colors) {
    my $cols = scalar @profileFiles;
    while ($cols) {
      push @$colors, '#009900';
      $cols--;
    }
  }

  my $rColorsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($colors, 'the.colors');

  my $pointsPch = $self->getPointsPch;
  my $rPointsPchString = ApiCommonWebsite::View::GraphPackage::Util::rNumericVectorFromArray($pointsPch, 'points.pch');
#  TODO: Determine if this is a property of the PlotSet or the PlotPart. May need to set from MixedPlotSet
#  my $legend = $profileSetsHash->{$part}->{legend};
#  my $rLegendString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($legend, 'the.legend');

  my $rCode = $self->rString($profileFilesString, $elementNamesString, $rColorsString, $rPointsPchString);

  return $rCode;
}

#--------------------------------------------------------------------------------

sub rString {
  my ($self, $profileFiles, $elementNamesFiles, $colorsString, $pointsPchString) = @_;

  my $rAdjustProfile = $self->getAdjustProfile();
  my $yAxisLabel = $self->getYaxisLabel();
  my $xAxisLabel = $self->getXaxisLabel();
  my $plotTitle = $self->getPlotTitle();

  my $yMax = $self->getDefaultYMax();
  my $yMin = $self->getDefaultYMin();

  my $xMax = $self->getDefaultXMax();
  my $xMin = $self->getDefaultXMin();

  my $yAxisFoldInductionFromM = $self->getMakeYAxisFoldInduction();

  my $pointsLast = $self->getArePointsLast();
  my $rTopMarginTitle = $self->getRTopMarginTitle();

  my $smoothLines = $self->getSmoothLines();
  my $splineApproxN = $self->getSplineApproxN();

  $yMax = $yMax ? $yMax : "-Inf";
  $yMin = defined($yMin) ? $yMin : "Inf";

  $xMax = $xMax ? $xMax : "-Inf";
  $xMin = defined($xMin) ? $xMin : "Inf";

  $pointsLast = defined($pointsLast) ? 'TRUE' : 'FALSE';

  $smoothLines = defined($smoothLines) ? 'TRUE' : 'FALSE';

  $yAxisFoldInductionFromM = defined($yAxisFoldInductionFromM) ? 'TRUE' : 'FALSE';

  my $forceNoLines = defined($self->getForceNoLines()) ? 'TRUE' : 'FALSE';
  my $varyGlyphByXAxis = defined($self->getVaryGlyphByXAxis()) ? 'TRUE' : 'FALSE';

  $rAdjustProfile = $rAdjustProfile ? $rAdjustProfile : "";
  $rTopMarginTitle = $rTopMarginTitle ? $rTopMarginTitle : "";

  $splineApproxN = defined($splineApproxN) ? $splineApproxN : 60;

  my $bottomMargin = $self->getBottomMarginSize();

  my $defaultPch = $self->getPointsPch()->[0];

  my $rv = "
# ---------------------------- LINE PLOT ----------------------------

$profileFiles
$elementNamesFiles
$colorsString
$pointsPchString

screen(screens[screen.i]);
screen.i <- screen.i + 1;
#-------------------------------------------------

if(length(profile.files) != length(element.names.files)) {
  stop(\"profile.files length not equal to element.names.files length\");
}

x.min = $xMin;
x.max = $xMax;

y.min = $yMin;
y.max = $yMax;

# Create Data Frames to collect values to be plotted
lines.df = as.data.frame(matrix(nrow=length(profile.files)));
lines.df\$V1 = NULL;

points.df = as.data.frame(matrix(nrow=length(profile.files)));
points.df\$V1 = NULL;

for(i in 1:length(profile.files)) {
  profile.df = read.table(profile.files[i], header=T, sep=\"\\t\");

  if(!is.null(profile.df\$ELEMENT_ORDER)) {
    profile.df = aggregate(profile.df, list(profile.df\$ELEMENT_ORDER), mean, na.rm=T)
  }
  profile = profile.df\$VALUE;

  element.names.df = read.table(element.names.files[i], header=T, sep=\"\\t\");
  element.names = as.character(element.names.df\$NAME);

# allow minor adjustments to profile
$rAdjustProfile

  element.names.numeric = as.numeric(sub(\" *[a-z-A-Z]+ *\", \"\", element.names, perl=T));
   is.numeric.element.names = !is.na(element.names.numeric);

  if($forceNoLines) {
    element.names.numeric = NA;
    is.numeric.element.names = is.numeric.element.names == 'BANANAS';
  }

  for(j in 1:length(element.names)) {
    this.name = element.names[j];
    this.name.numeric = as.character(element.names.numeric[j]);

    if(!is.na(this.name.numeric)) {
      this.name = this.name.numeric;
    }

    if(is.null(.subset2(points.df, this.name, exact=TRUE))) {
      points.df[[this.name]] = NA;
    }

    if(is.null(.subset2(lines.df, this.name, exact=TRUE))) {
      lines.df[[this.name]] = NA;
    }

    if(is.numeric.element.names[j]) {
      lines.df[[this.name]][i] = profile[j];

    } else {
      points.df[[this.name]][i] = profile[j];
    }
  }
}

isTimeSeries = FALSE;

x.coords = as.numeric(sub(\" *[a-z-A-Z]+ *\", \"\", colnames(lines.df), perl=T));
x.coords.rank = rank(x.coords, na.last=$pointsLast);


# if the points df is all NA's that means we can plot as Time Series
if(sum(is.na(points.df)) == ncol(points.df) * nrow(points.df)) {
  x.min = min(x.min, x.coords, na.rm=TRUE);
  x.max = max(x.max, x.coords+x.coords*.1, na.rm=TRUE);

  y.max = max(y.max, max(lines.df, na.rm=T), na.rm=TRUE);
  y.min = min(y.min, min(lines.df, na.rm=T), na.rm=TRUE);

  x.coords = sort(x.coords);

  isTimeSeries = TRUE;
} else {
  x.min = 1;
  x.max = length(x.coords);

  if(sum(is.na(lines.df)) == ncol(lines.df) * nrow(lines.df)) {
    y.max = max(y.max, max(points.df, na.rm=T), na.rm=TRUE);
    y.min = min(y.min, min(points.df, na.rm=T), na.rm=TRUE);

  } else {
    y.max = max(y.max, max(points.df, na.rm=T), max(lines.df, na.rm=T), na.rm=TRUE);
    y.min = min(y.min, min(points.df, na.rm=T), min(lines.df, na.rm=T), na.rm=TRUE);
  }
  x.coords = seq(x.min, x.max);
  if($forceNoLines) {
    x.coords.rank = x.coords;
  }
}

new.points = as.data.frame(matrix(NA, ncol=ncol(points.df), nrow=nrow(points.df)));
new.lines = as.data.frame(matrix(NA, ncol=ncol(lines.df), nrow=nrow(lines.df)));

for(j in 1:length(x.coords.rank)) {
  colRank = x.coords.rank[j];

  new.lines[[colRank]] = lines.df[[j]];
  new.points[[colRank]] = points.df[[j]];

  colnames(new.lines)[colRank] = colnames(lines.df)[j];
  colnames(new.points)[colRank] = colnames(points.df)[j];
}

par(mar       = c($bottomMargin,4,2,4), xpd=TRUE);

my.pch = $defaultPch;

for(i in 1:nrow(lines.df)) {

  if(!is.null(points.pch)) {
    my.pch = points.pch[i];
  }

  if(i == 1) {
    plot(x.coords,
         type = \"n\",
         xlab = \"$xAxisLabel\",
         xlim = c(x.min, x.max),
         xaxt = \"n\",
         ylab = \"$yAxisLabel\",
         ylim = c(y.min, y.max),
         axes = FALSE
        );


    if(isTimeSeries) {
      axis(1);

    } else {
      my.las = 2;
      if(max(nchar(colnames(lines.df))) < 6) {
        my.las = 0;
      }

      axis(1, at=x.coords.rank, labels=colnames(lines.df), las=my.las);
    }
  }

  # To have connected lines... you can't have NA's
  y.coords = new.lines[i,];
  colnames(y.coords) = as.character(x.coords);

  y.coords = y.coords[,!is.na(colSums(y.coords))];
  x.coords.line = as.numeric(sub(\" *[a-z-A-Z]+ *\", \"\", colnames(y.coords), perl=T));

  if($smoothLines) {
    points(x.coords.line,
         y.coords,
         col  = 'grey75',
         bg   = 'grey75',
         type = \"p\",
         pch  = my.pch,
         cex  = 0.5
         );

    lines(x.coords.line,
         y.coords,
         col  = 'grey75',
         bg  = 'grey75',
         cex  = 0.5
         );

    approxInterp = approx(x.coords.line, n=$splineApproxN);
    predict_x = approxInterp\$y;

    lines(predict(smooth.spline(x=x.coords.line, y=y.coords),predict_x),
         col  = the.colors[i],
         bg   = the.colors[i],
         cex  = 1
         );

  } else {
    lines(x.coords.line,
         y.coords,
         col  = the.colors[i],
         bg   = the.colors[i],
         type = \"o\",
         pch  = my.pch,
         cex  = 1
         );
  }


  my.color = the.colors[i];
  if($varyGlyphByXAxis) {
    my.pch = points.pch;
    my.color = the.colors;
  }


  points(x.coords,
       new.points[i,],
       col  = my.color,
       bg   = my.color,
       type = \"p\",
       pch  = my.pch,
       cex  = 1
       );
}

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
box();

$rTopMarginTitle


par(xpd=FALSE);
grid(nx=NA,ny=NULL,col=\"gray75\");
lines (c(0,length(profile) * 2), c(0,0), col=\"gray25\");


plasmodb.title(\"$plotTitle\");

";
  return $rv;
}

1;

#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile;
use base qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
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

#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio;
use base qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
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

#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::LinePlot::RMA;
use base qw( ApiCommonWebsite::View::GraphPackage::LinePlot );
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
