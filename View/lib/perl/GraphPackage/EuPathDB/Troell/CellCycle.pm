package ApiCommonWebsite::View::GraphPackage::EuPathDB::Troell::CellCycle;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors= ['darkgreen'];

  my @profileSetsArray = (['Troell Cell Cycle', 'standard error - Troell Cell Cycle', ]);
  my @percentileSetsArray = (['percentile - Troell Cell Cycle', '',]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $expression = EbrcWebsiteCommon::View::GraphPackage::LinePlot::QuantileNormalized->new(@_);
  $expression->setProfileSets($profileSets);
  $expression->setColors([$colors->[0]]);
  $expression->setElementNameMarginSize (5);
  $expression->setXaxisLabel("Hours");
  $expression->setAdjustProfile('lines.df = lines.df - lines.df[,1];');
  $expression->setDefaultYMin(-1);
  $expression->setDefaultYMax(1);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (5);
  $percentile->setXaxisLabel("Hours");

  $self->setGraphObjects($expression, $percentile,);

  return $self;
}



1;
