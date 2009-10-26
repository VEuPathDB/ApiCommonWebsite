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

    my $yAxisLabel = $profileSetsHash->{$part}->{y_axis_label};
    my $xAxisLabel = $profileSetsHash->{$part}->{x_axis_label};
    my $plotTitle = $profileSetsHash->{$part}->{plot_title};

    my $yMax = $profileSetsHash->{$part}->{default_y_max};
    my $yMin = $profileSetsHash->{$part}->{default_y_min};

    my $rCode = $self->rString($plotTitle, $profileFilesString, $elementNamesString, $rColorsString, $yAxisLabel, $xAxisLabel, $yMax, $yMin);

    unshift @rv, $rCode;
  }

  return \@rv;
}

#--------------------------------------------------------------------------------

sub rString {
  my ($self, $plotTitle, $profileFiles, $elementNamesFiles, $colorsString, $yAxisLabel, $xAxisLabel, $yMax, $yMin) = @_;

  $yAxisLabel = $yAxisLabel ? $yAxisLabel : "Whoops! no y_axis_label";
  $xAxisLabel = $xAxisLabel ? $xAxisLabel : "Whoops! no x_axis_label";
  $plotTitle = $plotTitle ? $plotTitle : "Whoops! You forgot the plot_title";

  $yMax = $yMax ? $yMax : "-Inf";
  $yMin = defined($yMin) ? $yMin : "Inf";

  my $bottomMargin = $self->getBottomMarginSize();

  my $rv = "
# ---------------------------- LINE PLOT ----------------------------

$profileFiles
$elementNamesFiles
$colorsString

screen(screens[screen.i]);
screen.i <- screen.i + 1;
#-------------------------------------------------

if(length(profile.files) != length(element.names.files)) {
  stop(\"profile.files length not equal to element.names.files length\");
}

element.names = list();
profile = list();

x.min = Inf;
x.max = -Inf;

y.min = $yMin;
y.max = $yMax;

for(i in 1:length(profile.files)) {
  profile.tmp = read.table(profile.files[i], header=T, sep=\"\\t\");
  profile[[i]] = profile.tmp\$VALUE;

  profile.max = max(profile[[i]]);
  profile.min = min(profile[[i]]);

  if(profile.max > y.max) y.max = profile.max;
  if(profile.min < y.min) y.min = profile.min;

  element.names.tmp = read.table(element.names.files[i], header=T, sep=\"\\t\");
  element.names[[i]] = as.numeric(sub(\" *[a-z-A-Z]+ *\", \"\", element.names.tmp\$NAME, perl=T));

  element.max = max(element.names[[i]]);
  element.min = min(element.names[[i]]);

  if(element.max > x.max) x.max = element.max;
  if(element.min < x.min) x.min = element.min;
}

#plasmodb.par
par(mar       = c($bottomMargin,4,1,10), xpd=TRUE);

for(i in 1:length(profile)) {

  if(i == 1) {
    plot(element.names[[i]],
         profile[[i]],
         col  = the.colors[i],
         bg   = the.colors[i],
         type = \"b\",
         pch  = 19,
         cex  = 1.5,
         xlab = \"$xAxisLabel\",
         xlim = c(x.min, x.max),
         ylab = \"$yAxisLabel\",
         ylim = c(y.min, y.max)
        );
  }
  else {
    lines(element.names[[i]],
         profile[[i]],
         col  = the.colors[i],
         bg   = the.colors[i],
         type = \"o\",
         pch  = 19,
         cex  = 1.5
         );
  }
}

#plasmodb.grid();
plasmodb.title(\"$plotTitle\");

";

  return $rv;
}





1;
