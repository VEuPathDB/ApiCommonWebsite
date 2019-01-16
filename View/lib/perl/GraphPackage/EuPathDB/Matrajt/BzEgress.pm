package ApiCommonWebsite::View::GraphPackage::EuPathDB::Matrajt::BzEgress;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;



sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(450);

  my $colors = ['#FF0000', '#FF6600', '#009900', '#0000CC',];

  my $legend = ['Extracellular Tachyzoite', 'Wild Type: Post-Egress', '13P Mutant: Post-Egress', 'B7 Mutant: Post-Egress',];

  $self->setMainLegend({colors => ['#FF0000', '#FF6600', '#009900', '#0000CC'], short_names => $legend, cols=> 2});

  my @profileSetsArray = (['TgRH_Matrajt_GSE23174_Bz_11hr_Egress', 'standard error - TgRH_Matrajt_GSE23174_Bz_11hr_Egress', '']);
  my @percentileSetsArray = (['percentile - TgRH_Matrajt_GSE23174_Bz_11hr_Egress', '',''],);

 my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (10);
  $rma->setScreenSize(300);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (10);
  $percentile->setScreenSize(300);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}

1;
