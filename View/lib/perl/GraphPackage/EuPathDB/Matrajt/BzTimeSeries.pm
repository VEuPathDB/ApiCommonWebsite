package ApiCommonWebsite::View::GraphPackage::EuPathDB::Matrajt::BzTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);


  my $colors = [ '#E9967A', '#87CEFA', '#00BFFF', '#4169E1', '#0000FF', ];

  my $legend = ['Extracellular\nTachyzoite (0 hrs)', 'Bradyzoite (24 hrs)', 'Bradyzoite (36 hrs)',' Bradyzoite (48 hrs)', 'Bradyzoite (72 hrs)'];
  my $shortNames = ['Tachy 0HR', 'Brady 24HR', 'Brady 36HR', 'Brady 48HR', 'Brady 72HR'];


  $self->setMainLegend({colors => [ '#E9967A', '#87CEFA', '#00BFFF','#4169E1', '#0000FF', ], short_names => $legend, cols=> 3});

   my @profileSetsArray = (['TgRH_Matrajt_GSE23174_Bz_Time_Series', 'standard error - TgRH_Matrajt_GSE23174_Bz_Time_Series', '']);
  my @percentileSetsArray = (['percentile - TgRH_Matrajt_GSE23174_Bz_Time_Series', '',''],);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setSampleLabels($shortNames);
  $rma->setElementNameMarginSize (6);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setSampleLabels($shortNames);
  $percentile->setElementNameMarginSize (6);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}

1;
