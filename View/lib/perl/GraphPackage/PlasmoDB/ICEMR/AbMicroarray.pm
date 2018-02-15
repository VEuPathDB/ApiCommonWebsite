package ApiCommonWebsite::View::GraphPackage::PlasmoDB::ICEMR::AbMicroarray;

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

  my $colors = ["#440154FF", "#482173FF", "#433E85FF", "#38598CFF", "#2D708EFF", "#25858EFF", "#1E9B8AFF", "#2BB07FFF", "#51C56AFF", "#85D54AFF", "#C2DF23FF", "#FDE725FF"];
  
  my $facet = $self->getFacets();
  my $contXAxis = $self->getContXAxis();
 
  my @profileSetArray = (['Uganda East Africa ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['Amazonia Peru ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['South Pacific ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['Amazonia Brazil ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['India ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['Malawi ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['South Africa Zambia Dec ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['South Africa Zambia Jun ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['South Pacific PNG ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['Amazonia ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['Southern African ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis],
                         ['Southeast Asia ICEMR Profiles','values', '', '', '', '', '', $facet, '', '', $contXAxis]);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetArray);

  my $scatter = EbrcWebsiteCommon::View::GraphPackage::GGScatterPlot::LogRatio->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setColors($colors);
  $scatter->setLegendLabels(['Uganda East Africa ICEMR Profiles',
                             'Amazonia Peru ICEMR Profiles',
                             'South Pacific ICEMR Profiles',
                             'Amazonia Brazil ICEMR Profiles',
                             'India ICEMR Profiles',
                             'Malawi ICEMR Profiles',
                             'South Africa Zambia Dec ICEMR Profiles',
                             'South Africa Zambia Jun ICEMR Profiles',
                             'South Pacific PNG ICEMR Profiles',
                             'Amazonia ICEMR Profiles',
                             'Southern African ICEMR Profiles',
                             'Southeast Asia ICEMR Profiles']);

  $self->setGraphObjects($scatter);

  return $self;

}
