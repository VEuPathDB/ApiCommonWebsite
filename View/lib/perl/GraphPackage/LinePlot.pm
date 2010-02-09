package ApiCommonWebsite::View::GraphPackage::LinePlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::AbstractPlot );
use ApiCommonWebsite::View::GraphPackage::AbstractPlot;

#--------------------------------------------------------------------------------

sub init {
  my $self = shift;
  my $args = ref $_[0] ? shift : {@_};

  $self->SUPER::init($args);

  # Defaults
  $self->setScreenSize(200);
  $self->setBottomMarginSize(4.5);

  return $self;
}

#--------------------------------------------------------------------------------

sub makeRPlotStrings {
  my ($self) = @_;

  my @rv;

  my $profileSetsHash = $self->getProfileSetsHash();

  my $ms = $self->getMultiScreen();

  my %isVis_b = $ms->partIsVisible();

  foreach my $part (keys %$profileSetsHash) {
    next unless ($isVis_b{$part});

    my (@profileFiles, @elementNamesFiles);

    # each part can have several profile sets
    my $i = 0;
    foreach my $profileSetName (@{$profileSetsHash->{$part}->{profiles}}) {
      my $suffix = $part . $i;

      my ($profileFile, $elementNamesFile) = @{$self->writeProfileFiles($profileSetName, $suffix)};

      push(@profileFiles, $profileFile);
      push(@elementNamesFiles, $elementNamesFile);

      $i++;
    }

    my $profileFilesString = $self->rStringVectorFromArray(\@profileFiles, 'profile.files');
    my $elementNamesString = $self->rStringVectorFromArray(\@elementNamesFiles, 'element.names.files');

    my $colors = $profileSetsHash->{$part}->{colors};
    my $rColorsString = $self->rStringVectorFromArray($colors, 'the.colors');

    my $pointsPch = $profileSetsHash->{$part}->{points_pch};
    my $rPointsPchString = $self->rNumericVectorFromArray($pointsPch, 'points.pch');

    my $yAxisLabel = $profileSetsHash->{$part}->{y_axis_label};
    my $xAxisLabel = $profileSetsHash->{$part}->{x_axis_label};
    my $plotTitle = $profileSetsHash->{$part}->{plot_title};

    my $yMax = $profileSetsHash->{$part}->{default_y_max};
    my $yMin = $profileSetsHash->{$part}->{default_y_min};

    my $xMin = $profileSetsHash->{$part}->{default_x_min};
    my $xMax = $profileSetsHash->{$part}->{default_x_max};

    my $rCode = $self->rString($plotTitle, $profileFilesString, $elementNamesString, $rColorsString, $rPointsPchString, $yAxisLabel, $xAxisLabel, $yMax, $yMin, $xMax, $xMin);

    unshift @rv, $rCode;
  }

  return \@rv;
}

#--------------------------------------------------------------------------------

sub rString {
  my ($self, $plotTitle, $profileFiles, $elementNamesFiles, $colorsString, $pointsPchString, $yAxisLabel, $xAxisLabel, $yMax, $yMin, $xMax, $xMin) = @_;

  $yAxisLabel = $yAxisLabel ? $yAxisLabel : "Whoops! no y_axis_label";
  $xAxisLabel = $xAxisLabel ? $xAxisLabel : "Whoops! no x_axis_label";
  $plotTitle = $plotTitle ? $plotTitle : "Whoops! You forgot the plot_title";

  $yMax = $yMax ? $yMax : "-Inf";
  $yMin = defined($yMin) ? $yMin : "Inf";

  $xMax = $xMax ? $xMax : "-Inf";
  $xMin = defined($xMin) ? $xMin : "Inf";

  my $bottomMargin = $self->getBottomMarginSize();

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
  profile = profile.df\$VALUE;

  element.names.df = read.table(element.names.files[i], header=T, sep=\"\\t\");
  element.names = as.character(element.names.df\$NAME);
  is.numeric.element.names = !is.na(as.numeric(sub(\" *[a-z-A-Z]+ *\", \"\", element.names, perl=T)));


  for(j in 1:length(element.names)) {
    this.name = element.names[j];

    if(is.null(points.df[[this.name]])) {
      points.df[[this.name]] = NA;
    }

    if(is.null(lines.df[[this.name]])) {
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
x.coords.rank = rank(x.coords);

# if the points df is all NA's that means we can plot as Time Series
if(sum(is.na(points.df)) == ncol(points.df) * nrow(points.df)) {
  x.min = min(x.min, x.coords, na.rm=TRUE);
  x.max = max(x.max, x.coords, na.rm=TRUE);

  y.max = max(y.max, max(lines.df, na.rm=T), na.rm=TRUE);
  y.min = min(y.min, min(lines.df, na.rm=T), na.rm=TRUE);

  x.coords = sort(x.coords);

  isTimeSeries = TRUE;
} else {
  x.min = 1;
  x.max = length(x.coords);

  y.max = max(y.max, max(points.df, na.rm=T), max(lines.df, na.rm=T), na.rm=TRUE);
  y.min = min(y.min, min(points.df, na.rm=T), min(lines.df, na.rm=T), na.rm=TRUE);

  x.coords = seq(x.min, x.max);
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

par(mar       = c($bottomMargin,4,1,10), xpd=TRUE);

my.pch = 15;

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
         ylim = c(y.min, y.max)
        );

    if(isTimeSeries) {
      axis(1);
    } else {
      axis(1, at=x.coords.rank, labels=colnames(lines.df));
    }
  }

  # To have connected lines... you can't have NA's
  y.coords = new.lines[i,];
  colnames(y.coords) = as.character(x.coords);

  y.coords = y.coords[,!is.na(colSums(y.coords))];
  x.coords.line = as.numeric(sub(\" *[a-z-A-Z]+ *\", \"\", colnames(y.coords), perl=T));

  lines(x.coords.line,
       y.coords,
       col  = the.colors[i],
       bg   = the.colors[i],
       type = \"o\",
       pch  = my.pch,
       cex  = 1.5
       );

  points(x.coords,
       new.points[i,],
       col  = the.colors[i],
       bg   = the.colors[i],
       type = \"o\",
       pch  = my.pch,
       cex  = 1.5
       );
}

#plasmodb.grid();
plasmodb.title(\"$plotTitle\");

";

  return $rv;
}





1;
