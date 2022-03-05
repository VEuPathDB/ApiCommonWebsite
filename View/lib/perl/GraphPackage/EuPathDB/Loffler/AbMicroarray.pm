package ApiCommonWebsite::View::GraphPackage::EuPathDB::Loffler::AbMicroarray;

use strict;
use vars qw( @ISA);

use Data::Dumper;

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(600);
  $self->setScreenSize(300);

  my $colors = ['blue','green','red'];
  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();
  if ($facet->[0] eq 'na') {
    $facet->[0] = 'OGMS_0000073';
  }
  my $needXLab = 0;
  if ($contXAxis eq 'na') {
    $contXAxis = 'OBI_0001169';
    $needXLab = 1;
  }

  my @profileSetArray = 
    (
     ['Natural infection Vaccine Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
     ['Natural infection Random Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
     ['Natural infection Bioinformatical Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]
    );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot::LogRatio->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);
  $scatter->setYaxisLabel("Relative antigenicity");
  $scatter->setPlotTitle("");

  $self->setGraphObjects($scatter);

  return $self;

}
