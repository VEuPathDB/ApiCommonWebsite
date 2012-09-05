package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::CellCycle;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setBottomMarginSize(6.5);

  my $colors = ['#CD853F'];

  my $pch = [19];

  my $legend = ['Cell Cycle'];


  my $cellCycleTopMargin = "
lines(c(2,5.75), c(y.max + (y.max - y.min)*0.1, y.max + (y.max - y.min)*0.1)); 
text(4, y.max + (y.max - y.min)*0.16, 'S(1)');

lines(c(5,6.9), c(y.max + (y.max - y.min)*0.125, y.max + (y.max - y.min)*0.125));
text(5.3, y.max + (y.max - y.min)*0.2, 'M');
text(6.3, y.max + (y.max - y.min)*0.2, 'C');

lines(c(6.1,10.4), c(y.max + (y.max - y.min)*0.1, y.max + (y.max - y.min)*0.1));
text(8.5, y.max + (y.max - y.min)*0.16, 'G1');

lines(c(10,13.2), c(y.max + (y.max - y.min)*0.125, y.max + (y.max - y.min)*0.125));
text(11.25, y.max + (y.max - y.min)*0.2, 'S(2)');

lines(c(12,14), c(y.max + (y.max - y.min)*0.15, y.max + (y.max - y.min)*0.15));
text(12.3, y.max + (y.max - y.min)*0.22, 'M');
text(13.3, y.max + (y.max - y.min)*0.22, 'C');


";

  my @profileSetsArray = (['M.White Cell Cycle Microarray','', ''],
                         );
  my @percentileSetsArray = (['percentile - M.White Cell Cycle Microarray', '',''],
                            );


  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = ApiCommonWebsite::View::GraphPackage::LinePlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setPointsPch($pch);
  $rma->setDefaultYMax(10);
  $rma->setDefaultYMin(4);
  $rma->setSmoothLines(1);
  $rma->setSplineApproxN(61);
  $rma->setTitleLine(2.25);
  $rma->setRPostscript($cellCycleTopMargin);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setPointsPch($pch);
  $percentile->setSmoothLines(1);
  $percentile->setSplineApproxN(61);
  $percentile->setTitleLine(2.25);
  $percentile->setRPostscript($cellCycleTopMargin);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}

1;
