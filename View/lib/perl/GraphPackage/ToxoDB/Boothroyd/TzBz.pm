package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#8FBC8F'];

   my @profileSetsArray = (['expression profiles of T. gondii Matt_Tz-Bz time series', 'standard error - expression profiles of T. gondii Matt_Tz-Bz time series', '']);
  my @percentileSetsArray = (['percentile - expression profiles of T. gondii Matt_Tz-Bz time series', '',''],);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = ApiCommonWebsite::View::GraphPackage::LinePlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}

1;
