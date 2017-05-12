package ApiCommonWebsite::View::GraphPackage::GGBarPlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlotPart );
use ApiCommonWebsite::View::GraphPackage::PlotPart;
use ApiCommonWebsite::View::GraphPackage::Util;
use ApiCommonWebsite::View::GraphPackage;
use Data::Dumper;

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

sub getAxisLty                  { $_[0]->{'_axis_lty'                        }}
sub setAxisLty                  { $_[0]->{'_axis_lty'                        } = $_[1]}

sub getLas                      { $_[0]->{'_las'                             }}
sub setLas                      { $_[0]->{'_las'                             } = $_[1]}

sub getSkipStdErr                 { $_[0]->{'_skip_std_err'                      }}
sub setSkipStdErr                 { $_[0]->{'_skip_std_err'                      } = $_[1]}

#--------------------------------------------------------------------------------

sub new {
  my $class = shift;

   my $self = $class->SUPER::new(@_);

   $self->setSpaceBetweenBars(0.3);
  $self->setAxisPadding(1.1);
  $self->setSkipStdErr(0);
   return $self;
}

#--------------------------------------------------------------------------------
sub makeRPlotString {
  my ($self, $idType) = @_;

  my $sampleLabels = $self->getSampleLabels();
  my $sampleLabelsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($sampleLabels, 'x.axis.label');
  my $overrideXAxisLabels = scalar @$sampleLabels > 0 ? "TRUE" : "FALSE";
  my $skipStdErr = $self->getSkipStdErr() ? 'TRUE' : 'FALSE';

  my ($profileFiles, $elementNamesFiles, $stderrFiles);

  my $blankGraph = $self->blankPlotPart();

  eval{
   ($profileFiles, $elementNamesFiles, $stderrFiles) = $self->makeFilesForR($idType);
 };

  if($@) {
    return $blankGraph;
  }

  foreach(@{$self->getProfileSets()}) {



    if(scalar @{$_->errors()} > 0) {
      return $blankGraph;

    }
  }
  my $colors = $self->getColors();

  my $colorsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($colors, 'the.colors');
  my $colorsStringNotNamed = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArrayNotNamed($colors);

  my $rAdjustProfile = $self->getAdjustProfile();
  my $yAxisLabel = $self->getYaxisLabel();
  my $plotTitle = $self->getPlotTitle();

  my $yMax = $self->getDefaultYMax();
  my $yMin = $self->getDefaultYMin();

  my $axisLty = $self->getAxisLty();
  my $axisLtyString = defined($axisLty) ? 'TRUE' : 'FALSE';
  $axisLty = defined($axisLty)? $axisLty : 'NULL';

  my $las = $self->getLas();
  my $lasString = defined($las) ? 'TRUE' : 'FALSE';
  $las = defined($las) ? $las : 'NULL';

  my $isCompactString = "FALSE";

  if($self->isCompact()) {
    $isCompactString = "TRUE";
  }

  my $isStack = $self->getIsStacked() ? 'TRUE' : 'FALSE';
  my $isHorizontal = $self->getIsHorizontal();

  my $horizontalXAxisLabels = $self->getForceHorizontalXAxis();

  my $yAxisFoldInductionFromM = $self->getMakeYAxisFoldInduction();
  my $highlightMissingValues = $self->getHighlightMissingValues();

  $highlightMissingValues = $highlightMissingValues ? 'TRUE' : 'FALSE';

  $rAdjustProfile = $rAdjustProfile ? $rAdjustProfile : "";

  $horizontalXAxisLabels = $horizontalXAxisLabels ? 'TRUE' : 'FALSE';

  $yAxisFoldInductionFromM = $yAxisFoldInductionFromM ? 'TRUE' : 'FALSE';

  my $horiz = $isHorizontal && !$self->isCompact() ? 'TRUE' : 'FALSE';

  my $titleLine = $self->getTitleLine();

  my $bottomMargin = $self->getElementNameMarginSize();
  my $spaceBetweenBars = $self->getSpaceBetweenBars();

  my $scale = $self->getScalingFactor;

  my $hasExtraLegend = $self->getHasExtraLegend() ? 'TRUE' : 'FALSE';
  my $legendLabels = $self->getLegendLabels();

  my ($legendLabelsString, $legendColors, $legendColorsString);

  my $profileTypes = $self->getProfileTypes();
  my $profileTypesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($profileTypes, 'profile.types');

  my $facets = $self->getFacets();
  my $facetString = ". ~ DUMMY";
  my $hasFacets = "FALSE";
  if($facets && scalar @$facets == 1) {
    $facetString = ". ~ " . $facets->[0];
    $hasFacets = "TRUE";
  }
  if($facets && scalar @$facets == 2) {
    $facetString = $facets->[0] . " ~  " . $facets->[1];
    $hasFacets = "TRUE";
  }

  if ($hasExtraLegend ) {
      $legendLabelsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($legendLabels, 'legend.label');

      $legendColors = $self->getLegendColors();
      $legendColors = $colors if !($legendColors);
      $legendColorsString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray($legendColors, 'legend.colors');
    }

  my $hasLegendLabels = $legendLabelsString ? 'TRUE' : 'FALSE';

  my $extraLegendSize = $self->getExtraLegendSize();

  my $axisPadding = $self->getAxisPadding();

  my $rv = "
# ---------------------------- BAR PLOT ----------------------------

$profileFiles
$elementNamesFiles
$stderrFiles
$colorsString
$sampleLabelsString
$legendLabelsString
$legendColorsString
$profileTypesString

is.compact=$isCompactString;

#-------------------------------------------------------------------

if(length(profile.files) != length(element.names.files)) {
  stop(\"profile.files length not equal to element.names.files length\");
}

y.min = $yMin;
y.max = $yMax;

profile.df.full = data.frame();

for(ii in 1:length(profile.files)) {
  skip.stderr = FALSE;

  profile.df = read.table(profile.files[ii], header=T, sep=\"\\t\");
  profile.df\$Group.1=NULL

  if(!is.null(profile.df\$ELEMENT_ORDER)) {
    eo.count = length(profile.df\$ELEMENT_ORDER);
    if(!is.numeric(profile.df\$ELEMENT_ORDER)) {
      stop(\"Element order must be numeric for aggregation\");
    }

    profile.df = aggregate(profile.df, list(profile.df\$ELEMENT_ORDER), mean, na.rm=T)
    if(length(profile.df\$ELEMENT_ORDER) != eo.count) {
      skip.stderr = TRUE;
    }

    profile.df\$PROFILE_FILE = profile.files[ii];
    profile.df\$PROFILE_TYPE = profile.types[ii];

    if(length(profile.files) > 1) {
      profile.df\$LEGEND = legend.label[ii];
    }

  }

  element.names.df = read.table(element.names.files[ii], header=T, sep=\"\\t\");
  profile.df = merge(profile.df, element.names.df[, c(\"ELEMENT_ORDER\", \"NAME\")], by=\"ELEMENT_ORDER\")

  if(!skip.stderr && !is.na(stderr.files[ii]) && stderr.files[ii] != '') {
    stderr.df = read.table(stderr.files[ii], header=T, sep=\"\\t\");
    colnames(stderr.df) = c(\"ELEMENT_ORDER\", \"STDERR\");

    profile.df = merge(profile.df, stderr.df[, c(\"ELEMENT_ORDER\", \"STDERR\")], by=\"ELEMENT_ORDER\");
  } else {
    profile.df\$STDERR = NA;
  }

  profile.df.full = rbind(profile.df.full, profile.df);
}

profile.df.full\$MIN_ERR = profile.df.full\$VALUE - profile.df.full\$STDERR;
profile.df.full\$MAX_ERR = profile.df.full\$VALUE + profile.df.full\$STDERR;

if(length(profile.files) == 1) {
  profile.df.full\$LEGEND = legend.label;
  if($overrideXAxisLabels) {
    profile.df.full\$NAME = x.axis.label;    
  }
}

profile.df.full\$NAME <- factor(profile.df.full\$NAME, levels = profile.df.full\$NAME[order(profile.df.full\$ELEMENT_ORDER)])

expandColors = FALSE;

if(is.null(profile.df.full\$LEGEND)) {
  profile.df.full\$LEGEND = profile.df.full\$NAME
  expandColors = TRUE;
} else {
  profile.df.full\$LEGEND = factor(profile.df.full\$LEGEND, levels=legend.label);
}

# allow minor adjustments to profile
$rAdjustProfile

y.max = max(c(y.max, profile.df.full\$VALUE, profile.df.full\$MAX_ERR), na.rm=TRUE);
y.min = min(c(y.min, profile.df.full\$VALUE, profile.df.full\$MIN_ERR), na.rm=TRUE);

gp = ggplot(profile.df.full, aes(x=NAME, y=VALUE, fill=LEGEND, colour=LEGEND));


if($isStack) {
  gp = gp + geom_bar(stat=\"identity\", position=\"stack\", size=1.2);
} else {
  gp = gp + geom_bar(stat=\"identity\", position=\"dodge\", size=1.2);
}

if(expandColors) {
  gp = gp + scale_fill_manual(values=rep($colorsStringNotNamed, length(profile.df.full\$NAME)/length($colorsStringNotNamed)), breaks=profile.df.full\$LEGEND, name=NULL);
} else {
  gp = gp + scale_fill_manual(values=$colorsStringNotNamed, breaks=profile.df.full\$LEGEND, name=NULL);
}

gp = gp + scale_colour_discrete(breaks=profile.df.full\$LEGEND, name=NULL);

gp = gp + geom_errorbar(aes(ymin=MIN_ERR, ymax=MAX_ERR), colour=\"black\", width=.1);

if(is.compact) {
  gp = gp + theme_void() + theme(legend.position=\"none\");
} else {
  gp = gp + labs(title=\"$plotTitle\", y=\"$yAxisLabel\", x=NULL);
  gp = gp + ylim(y.min, y.max);
  gp = gp + scale_x_discrete(label=abbreviate);
  gp = gp + theme(axis.text.x  = element_text(angle=90,vjust=0.5, size=12), plot.title = element_text(colour=\"#b30000\"));

  if(length(the.colors) > 13) {
    gp = gp + guides(fill=guide_legend(ncol=2));
  }

}

if($horiz) {
  gp = gp + coord_flip();
}


gp = gp + geom_hline(yintercept = 0, colour=\"grey\")


if($hasFacets) {
  gp = gp + facet_grid($facetString);
}


plotlist[[plotlist.i]] = gp;
plotlist.i = plotlist.i + 1;





";

  return $rv;
}

1;

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::RMA;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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
# TODO:  WHAT TO DO ABOUT ERROR BARS??
    $self->setAdjustProfile('profile.df.full$VALUE = 2^(profile.df.full$VALUE);');
    $self->setYaxisLabel("RMA Value");
    $self->setSkipStdErr(1);
  }

  $self->setDefaultYMax(4);
  $self->setDefaultYMin(0);

  $self->setPartName('rma');
  $self->setPlotTitle("RMA Expression Value - $id");

  return $self;
}

1;

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::Percentile;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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



package ApiCommonWebsite::View::GraphPackage::GGBarPlot::RNASeqSpliced;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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




package ApiCommonWebsite::View::GraphPackage::GGBarPlot::RNASeq;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
use strict;

sub new {
  my $class = shift; 
  my $self = $class->SUPER::new(@_);

  my $id = $self->getId();
  my $wantLogged = $self->getWantLogged();

  $self->setPartName('fpkm');
  $self->setYaxisLabel('FPKM');
  $self->setIsStacked(0);
  $self->setDefaultYMin(0);
  $self->setDefaultYMax(10);
  $self->setPlotTitle("FPKM - $id");

  if($wantLogged) {
    $self->setAdjustProfile('profile.df.full$VALUE = log2(profile.df.full$VALUE + 1);');
    $self->setYaxisLabel('FPKM (log2)');
    $self->setIsLogged(1);
    $self->setDefaultYMax(4);
    $self->setSkipStdErr(1);
  }

  return $self;
}

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::PairedEndRNASeqStacked;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot::RNASeq);
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

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::LogRatio;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::QuantileNormalized;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::MRNADecay;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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


package ApiCommonWebsite::View::GraphPackage::GGBarPlot::Standardized;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::MassSpec;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
use strict;

sub new {
  my $class = shift; 
   my $self = $class->SUPER::new(@_);

   my $id = $self->getId();

   $self->setDefaultYMax(10);
   $self->setDefaultYMin(0);
   $self->setYaxisLabel('');

   $self->setPartName('mass_spec');
   $self->setPlotTitle("Mass Profile - $id");

   $self->setMakeYAxisFoldInduction(0);
   $self->setIsLogged(0);

   return $self;
}

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::QuantMassSpec;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::QuantMassSpecNonRatio;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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

package ApiCommonWebsite::View::GraphPackage::GGBarPlot::QuantMassSpecNonRatioUnlogged;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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



package ApiCommonWebsite::View::GraphPackage::GGBarPlot::SageTag;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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



package ApiCommonWebsite::View::GraphPackage::GGBarPlot::Genera;
use base qw( ApiCommonWebsite::View::GraphPackage::GGBarPlot );
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


