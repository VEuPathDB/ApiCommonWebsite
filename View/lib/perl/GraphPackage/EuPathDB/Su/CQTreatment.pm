package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Su::CQTreatment;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#F08080', '#7CFC00' ];
  my $legend = ['untreated', 'chloroquine'];
  my $pch = [22];
  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  my $untreated = ['106/1','','106/1 (76I)','', '106/1 (76I_352K)', ''];
  my $treated = ['', '106/1','','106/1 (76I)','', '106/1 (76I_352K)'];


  my @profileArray = (['E-GEOD-10022 array from Su', 'standard error - E-GEOD-10022 array from Su', $untreated],
                      ['E-GEOD-10022 array from Su', 'standard error - E-GEOD-10022 array from Su', $treated]
                     );

  my @percentileArray = (['percentile - E-GEOD-10022 array from Su', '', $untreated],
                         ['percentile - E-GEOD-10022 array from Su', '', $treated],
                        );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;
