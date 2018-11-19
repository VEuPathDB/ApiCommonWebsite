package ApiCommonWebsite::View::GraphPackage::EuPathDB::Matrajt::BzMutant;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(450);
  $self->setBottomMarginSize(7);

#BZ-Mutant Graphs
  my $mutant_colors = ['#996600', '#996600', '#996600', '#FF0000', '#FF0000', '#FF6600', '#FF6600','#FFFF00', '#FFFF00','#33FF66', '#33FF66', '#009900', '#009900', '#0000CC', '#0000CC', '#660033', '#660033',];

  my @mutantProfileSetsArray = (['TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant', 'standard error - TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant', '']);
  my @mutantPercentileSetsArray = (['percentile - TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant', '',''],);

  my $mutantProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@mutantProfileSetsArray);
  my $mutantPercentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@mutantPercentileSetsArray);

  my $mutant_rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $mutant_rma->setProfileSets($mutantProfileSets);
  $mutant_rma->setColors($mutant_colors);
  $mutant_rma->setElementNameMarginSize (10);
  $mutant_rma->setScreenSize(300);
  $mutant_rma->setPartName('bz_mutant_rma');

  my $mutant_percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $mutant_percentile->setProfileSets($mutantPercentileSets);
  $mutant_percentile->setColors($mutant_colors);
  $mutant_percentile->setElementNameMarginSize (10);
  $mutant_percentile->setScreenSize(300);
  $mutant_percentile->setPartName('bz_mutant_percentile');

#BZ-Egress Graphs
  my $egress_colors = ['#FF0000', '#FF6600', '#009900', '#0000CC',];

  my @egressProfileSetsArray = (['TgRH_Matrajt_GSE23174_Bz_11hr_Egress', 'standard error - TgRH_Matrajt_GSE23174_Bz_11hr_Egress', '']);
  my @egressPercentileSetsArray = (['percentile - TgRH_Matrajt_GSE23174_Bz_11hr_Egress', '',''],);

  my $egressProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@egressProfileSetsArray);
  my $egressPercentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@egressPercentileSetsArray);

  my $egress_rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $egress_rma->setProfileSets($egressProfileSets);
  $egress_rma->setColors($egress_colors);
  $egress_rma->setElementNameMarginSize (10);
  $egress_rma->setScreenSize(300);
  $egress_rma->setPartName('bz_egress_rma');

  my $egress_percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $egress_percentile->setProfileSets($egressPercentileSets);
  $egress_percentile->setColors($egress_colors);
  $egress_percentile->setElementNameMarginSize (10);
  $egress_percentile->setScreenSize(300);
  $egress_percentile->setPartName('bz_egress_percentile');

#BZ-Time Series Graphs


  my $ts_colors = [ '#E9967A', '#87CEFA', '#00BFFF', '#4169E1', '#0000FF', ];

  my $ts_shortNames = ['Tachy 0HR', 'Brady 24HR', 'Brady 36HR', 'Brady 48HR', 'Brady 72HR'];

  my @tsProfileSetsArray = (['TgRH_Matrajt_GSE23174_Bz_Time_Series', 'standard error - TgRH_Matrajt_GSE23174_Bz_Time_Series', '']);
  my @tsPercentileSetsArray = (['percentile - TgRH_Matrajt_GSE23174_Bz_Time_Series', '',''],);

  my $tsProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@tsProfileSetsArray);
  my $tsPercentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@tsPercentileSetsArray);
  my $ts_rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $ts_rma->setProfileSets($tsProfileSets);
  $ts_rma->setColors($ts_colors);
  $ts_rma->setSampleLabels($ts_shortNames);
  $ts_rma->setElementNameMarginSize (6);
  $ts_rma->setPartName('bz_time_series_rma');

  my $ts_percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $ts_percentile->setProfileSets($tsPercentileSets);
  $ts_percentile->setColors($ts_colors);
  $ts_percentile->setSampleLabels($ts_shortNames);
  $ts_percentile->setElementNameMarginSize (6);
  $ts_percentile->setPartName('bz_time_series_percentile');


  $self->setGraphObjects( $mutant_rma, $mutant_percentile,$egress_rma, $egress_percentile,$ts_rma, $ts_percentile);

  return $self;
}

1;
