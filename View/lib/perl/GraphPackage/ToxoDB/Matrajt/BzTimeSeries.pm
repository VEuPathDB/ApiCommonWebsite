package ApiCommonWebsite::View::GraphPackage::ToxoDB::Matrajt::BzTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(450);

  my $colors = [ '#E9967A', '#87CEFA', '#00BFFF', '#4169E1', '#0000FF', ];

  my $legend = ['Extracellular\nTachyzoite (0 hrs)', 'Bradyzoite (24 hrs)', 'Bradyzoite (36 hrs)',' Bradyzoite (48 hrs)', 'Bradyzoite (72 hrs)'];

  $self->setMainLegend({colors => [ '#E9967A', '#87CEFA', '#00BFFF','#4169E1', '#0000FF', ], short_names => $legend, cols=> 3});

   my @profileSetsArray = (['TgRH_Matrajt_GSE23174_Bz_Time_Series', 'standard error - TgRH_Matrajt_GSE23174_Bz_Time_Series', '']);
  my @percentileSetsArray = (['percentile - TgRH_Matrajt_GSE23174_Bz_Time_Series', '',''],);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (10);
  $rma->setScreenSize(300);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (10);
  $percentile->setScreenSize(300);

  $self->setGraphObjects($rma, $percentile);

  return $self;
}

1;
