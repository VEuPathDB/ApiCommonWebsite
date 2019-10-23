package ApiCommonWebsite::View::GraphPackage::EuPathDB::LOPIT::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;


use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(515);
  my $colors = ['#4682B4', '#B22222'];

  my @profileSetsArray = (['LOPIT - MAP', 'values', '', ''],
                          ['LOPIT - MCMC', 'values', '', '']);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $cl = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::LOPIT->new(@_);
  $cl->setProfileSets($profileSets);
  $cl->setColors($colors);

  $cl->setLegendColors($colors);
  $cl->setLegendLabels(["MAP", "MCMC"]);


  $self->setGraphObjects($cl);

  return $self;
}



1;
