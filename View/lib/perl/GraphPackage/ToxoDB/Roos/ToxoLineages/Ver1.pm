package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::ToxoLineages::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(450);

  my $colors = ['#4682B4', '#B22222', '#8FBC8F', '#6A5ACD', '#87CEEB', '#CD853F',];

  my @profileSetsArray = (['Expression profiling of T. gondii strains', 'standard error - Expression profiling of T. gondii strains', '']);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setDefaultYMax(10);
  $rma->setSpaceBetweenBars(0.3);
  $rma->setPlotTitle('Tachyzoite comparison of archetypal T.gondii lineages');

  $self->setGraphObjects($rma);

  return $self;
}



1;
