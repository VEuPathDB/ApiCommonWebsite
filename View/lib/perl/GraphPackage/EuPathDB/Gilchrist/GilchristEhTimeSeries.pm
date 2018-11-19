package ApiCommonWebsite::View::GraphPackage::AmoebaDB::Gilchrist::GilchristEhTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  my $colors =['#800517', '#307D7E','#254117', '#7E3517', '#806517'];

  my $xAxisLabels = ['HM-1:IMSS', 'Rahman','HM-1:IMSS-MA', 'HM-1:IMSS-MA 1d PI','HM-1:IMSS-MA 29d PI'];

  my $legend = ["HM-1:IMSS - Trophs TYI", "Rahman Trophs TYI","HM1:IMSS mouse-adapted-Trophs TYI", "HM1:IMSS mouse-adapted - Trophs 1d PI","HM1:IMSS mouse-adapted - Trophs 29d PI"];

   $self->setMainLegend({colors => $colors, short_names => $legend,cols => 1});

  my @profileSetsArray = (['E. histolytica Impact of intestinal colonization and invasion on transcriptome', 'standard error - E. histolytica Impact of intestinal colonization and invasion on transcriptome',$xAxisLabels ]);
  my @percentileSetsArray = (['percentile - E. histolytica Impact of intestinal colonization and invasion on transcriptome', '', $xAxisLabels ],);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (9);
  $rma->setScreenSize(300);
  $rma->setDefaultYMax(12);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (9);
  $percentile->setScreenSize(300);

  $self->setGraphObjects($rma, $percentile,);

  return $self;

}


1;
