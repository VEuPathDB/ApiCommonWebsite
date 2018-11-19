package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Sage::McArthur;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;
use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @legendColors = ('#A52A2A', '#DEB887');

  my $legend = ["sense", "antisense"];

  my @profileSetsArray = (['giar sage tag frequencies sense', 'values',''],
                          ['giar sage tag frequencies antisense', 'values','']);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $percents = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::SageTag->new(@_);
  $percents->setProfileSets($profileSets);
  $percents->setColors(\@legendColors);
  $percents->setElementNameMarginSize(8.5);


  $percents->setHasExtraLegend(1); 
  $percents->setLegendLabels($legend);


  $self->setGraphObjects($percents);

  return $self;

}



1;
