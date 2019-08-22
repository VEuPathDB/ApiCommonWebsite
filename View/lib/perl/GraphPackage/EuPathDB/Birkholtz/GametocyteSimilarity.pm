package ApiCommonWebsite::View::GraphPackage::EuPathDB::Birkholtz::GametocyteSimilarity;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;


use Data::Dumper;
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'grey');
  my @legend = ('Match', 'Query');
  my @sampleNames = ('2-day pre-induction','1-day pre-induction','0-day pre-induction','1-day post induction','2-day post induction','3-day post induction','4-day post induction','5-day post induction','6-day post induction','7-day post induction','8-day post induction','9-day post induction','10-day post induction','11-day post induction','12-day post induction','13-day post induction');

 

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 2});
  $self->setPlotWidth(450);

  # Need to make 2 Profiles ... one for the primaryID and one for the Secondary
  my @profileArray = (['Pfal3D7 Gametocyte time course','values'],
                      ['Pfal3D7 Gametocyte time course','values'],
                     );


  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $similarity = EbrcWebsiteCommon::View::GraphPackage::SimilarityPlot::LogRatio->new(@_);
  $similarity->setProfileSets($profileSets);
  $similarity->setColors(\@colors);
  $similarity->setPointsPch([15,15]);
  $similarity->setElementNameMarginSize(5);
  $similarity->setSampleLabels(\@sampleNames);
  $similarity->setXaxisLabel("");

  $self->setGraphObjects($similarity);

  return $self;
}

1;

