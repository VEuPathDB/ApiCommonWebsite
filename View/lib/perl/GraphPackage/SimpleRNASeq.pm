package ApiCommonWebsite::View::GraphPackage::SimpleRNASeq;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::Util;

sub getMinRpkmProfileSet { $_[0]->{_min_rpkm_profile_set} }
sub setMinRpkmProfileSet { $_[0]->{_min_rpkm_profile_set} = $_[1] }

sub getDiffRpkmProfileSet { $_[0]->{_diff_rpkm_profile_set} }
sub setDiffRpkmProfileSet { $_[0]->{_diff_rpkm_profile_set} = $_[1] }

sub getPctProfileSet { $_[0]->{_pct_profile_set} }
sub setPctProfileSet { $_[0]->{_pct_profile_set} = $_[1] }

sub getAdditionalRCode { $_[0]->{_additional_r_code} }
sub setAdditionalRCode { $_[0]->{_additional_r_code} = $_[1] }

sub getColor { $_[0]->{_color} }
sub setColor { $_[0]->{_color} = $_[1] }

sub getSampleNames { $_[0]->{_sample_names} }
sub setSampleNames { $_[0]->{_sample_names} = $_[1] }


sub setBottomMarginSize {
  my ($self, $ms) = @_;

  # set the margin size for the mixed plot
  $self->SUPER::setBottomMarginSize($ms);


  # set the margin size for each of the parts if already set
  my $graphObjects = $self->getGraphObjects();
  foreach(@{$graphObjects}) {
    $_->setElementNameMarginSize($ms);
  }

}


sub makeGraphs {
  my $self = shift;

  my $minRpkmProfileSet = $self->getMinRpkmProfileSet();
  my $diffRpkmProfileSet = $self->getDiffRpkmProfileSet();
  my $pctProfileSet = $self->getPctProfileSet();
  my $additionalRCode = $self->getAdditionalRCode();
  my $color = $self->getColor() ? $self->getColor() : 'blue';

  my $sampleNames = $self->getSampleNames();

  my @colors = ($color, '#DDDDDD');
  my @legend = ("Uniquely Mapped", "Non-Uniquely Mapped");

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});

  my @profileArray = ([$minRpkmProfileSet, '', $sampleNames],
                      [$diffRpkmProfileSet, '', $sampleNames],
                     );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets([[$pctProfileSet, '', $sampleNames]]);

  my $stacked = ApiCommonWebsite::View::GraphPackage::BarPlot::RNASeqStacked->new(@_);
  $stacked->setProfileSets($profileSets);
  $stacked->setColors(\@colors);
  $stacked->addAdjustProfile($additionalRCode);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors([$colors[0]]);
  $percentile->addAdjustProfile($additionalRCode);

  if(my $bottomMargin = $self->getBottomMarginSize()) {
    $stacked->setElementNameMarginSize($bottomMargin);
    $percentile->setElementNameMarginSize($bottomMargin);
  }


  $self->setGraphObjects($stacked, $percentile);

  return $self;
}


1;
