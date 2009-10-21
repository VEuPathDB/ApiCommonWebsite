package ApiCommonWebsite::View::GraphPackage::BarPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::AbstractPlot );
use ApiCommonWebsite::View::GraphPackage::AbstractPlot;

use Data::Dumper;

#--------------------------------------------------------------------------------

sub getScreenSize            { $_[0]->{'_screen_size'         }}
sub setScreenSize            { $_[0]->{'_screen_size'         } = $_[1]; $_[0] }

#--------------------------------------------------------------------------------

sub init {
  my $self = shift;
  my $args = ref $_[0] ? shift : {@_};

  $self->SUPER::init($args);

  # Defaults
  $self->setScreenSize(250);

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

    my $colors = $profileSetsHash->{$part}->{colors};
    my $rColorsString = $self->rStringVectorFromArray($colors, 'the.colors');

    my $rXAxisLabelsString;
    if(my $xAxisLabels = $profileSetsHash->{$part}->{x_axis_labels}) {
      $rXAxisLabelsString = $self->rStringVectorFromArray($xAxisLabels, 'element.names');
    }

    my $legend = $profileSetsHash->{$part}->{legend};
    my $rLegendString = $self->rStringVectorFromArray($legend, 'the.legend');

    my $rAdjustProfile = $profileSetsHash->{$part}->{r_adjust_profile};
    my $yAxisLabel = $profileSetsHash->{$part}->{y_axis_label};
    my $plotTitle = $profileSetsHash->{$part}->{plot_title};

    # each part can have several profile sets
    foreach my $profileSetName (@{$profileSetsHash->{$part}->{profiles}}) {
      my ($profileFile, $elementNamesFile) = @{$self->writeProfileFiles($profileSetName, $part, undef)};

      push(@profileFiles, $profileFile);
      push(@elementNamesFiles, $elementNamesFile);
    }
    my $profileFilesString = $self->rStringVectorFromArray(\@profileFiles, 'profile.files');
    my $elementNamesString = $self->rStringVectorFromArray(\@elementNamesFiles, 'element.names.files');

    my $rCode = $self->rString($plotTitle, $profileFilesString, $elementNamesString, $rColorsString, $rLegendString, $yAxisLabel, $rXAxisLabelsString, $rAdjustProfile);

    unshift @rv, $rCode;
  }

  return \@rv;
}

#--------------------------------------------------------------------------------

sub rString {
  my ($self, $plotTitle, $profileFiles, $elementNamesFiles, $colorsString, $legend, $yAxisLabel, $rAdjustNames, $rAdjustProfile) = @_;

  $yAxisLabel = $yAxisLabel ? $yAxisLabel : "Whoops! no y_axis_label";
  $plotTitle = $plotTitle ? $plotTitle : "Whoops! You forgot the plot_title";
  $rAdjustProfile = $rAdjustProfile ? $rAdjustProfile : "";
  $rAdjustNames = $rAdjustNames ? $rAdjustNames : "";

  my $rv = "
# ---------------------------- BAR PLOT ----------------------------

$profileFiles
$elementNamesFiles
$colorsString
$legend

screen(screens[screen.i]);
screen.i <- screen.i + 1;

profile = vector();
for(i in 1:length(profile.files)) {
  tmp = read.table(profile.files[i], header=T, sep=\"\\t\");
  profile = rbind(profile, tmp\$VALUE);
}

element.names = vector(\"character\");
for(i in 1:length(element.names.files)) {
  tmp = read.table(element.names.files[i], header=T, sep=\"\\t\");
  element.names = rbind(element.names, as.vector(tmp\$NAME));
}

par(mar       = c(8,4,1,10), xpd=TRUE);

# Allow Subclass to fiddle with the data structure and x axis names
$rAdjustProfile
$rAdjustNames

d.max = max(1.1 * profile, 10);

barplot(profile,
        col       = the.colors,
        ylab      = '$yAxisLabel',
        ylim      = c(0, d.max),
        beside    = TRUE,
        names.arg = element.names,
        space=c(0,.5),
        las = 2
       );

if(length(the.legend) > 0) {
  legend(11, d.max, legend=the.legend, cex=0.9, fill=the.colors, inset=0.2) ;
}

plasmodb.title(\"$plotTitle\");

box();


";

  return $rv;
}





1;
