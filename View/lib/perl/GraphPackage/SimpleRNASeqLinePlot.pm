package ApiCommonWebsite::View::GraphPackage::SimpleRNASeqLinePlot;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimpleRNASeq );
use ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

use ApiCommonWebsite::View::GraphPackage::LinePlot;
use ApiCommonWebsite::View::GraphPackage::Util;


sub getXAxisLabel { $_[0]->{_x_axis_label} }
sub setXAxisLabel { $_[0]->{_x_axis_label} = $_[1] }

sub makeGraphs {
  my $self = shift;

  my $minRpkmProfileSet = $self->getMinRpkmProfileSet();
  my $diffRpkmProfileSet = $self->getDiffRpkmProfileSet();
  my $pctProfileSet = $self->getPctProfileSet();
  my $additionalRCode = $self->getAdditionalRCode();
  my $color = $self->getColor() ? $self->getColor() : 'blue';

  my $sampleNames = $self->getSampleNames();

  my @colors = ('#DDDDDD', $color);
  my @legend = ("Non-Uniquely Mapped", "Uniquely Mapped");

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});

  # Draw the diff first in light grey ... then the min rpkm will go on top
  my @profileArray = ([$diffRpkmProfileSet, '', $sampleNames],
                      [$minRpkmProfileSet, '', $sampleNames],
                     );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([[$pctProfileSet, '', $sampleNames]]);


  $additionalRCode = "lines.df[1,] = lines.df[1,] + lines.df[2,];\n$additionalRCode";

  my $stacked = ApiCommonWebsite::View::GraphPackage::LinePlot::RNASeq->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);
  $stacked->addAdjustProfile($additionalRCode);
  $stacked->setXaxisLabel($self->getXAxisLabel());

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[1]]);
  $percentile->addAdjustProfile($additionalRCode);

  if(my $bottomMargin = $self->getBottomMarginSize()) {
    $stacked->setElementNameMarginSize($bottomMargin);
    $percentile->setElementNameMarginSize($bottomMargin);
  }


  $self->setGraphObjects($stacked, $percentile);

  return $self;
}

