package ApiCommonWebsite::View::GraphPackage::EuPathDB::Roos::ToxoLineages::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  my $colors = ['#4682B4', '#B22222', '#8FBC8F', '#6A5ACD', '#87CEEB', '#CD853F',];

  my @profileSetsArray = (['Expression profiling of T. gondii strains', 'standard error - Expression profiling of T. gondii strains', '']);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setPlotTitle('Tachyzoite comparison of archetypal T.gondii lineages');

  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['percentile - Expression profiling of T. gondii strains']]);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  
  $self->setGraphObjects($rma, $percentile);


  return $self;
}



1;
