package ApiCommonWebsite::View::GraphPackage::ToxoDB::Matrajt::BzMutant;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(450);
  $self->setBottomMarginSize(7);

  my $colors = ['#996600', '#996600', '#996600', '#FF0000', '#FF0000', '#FF6600', '#FF6600','#FFFF00', '#FFFF00','#33FF66', '#33FF66', '#009900', '#009900', '#0000CC', '#0000CC', '#660033', '#660033',];

  my $legend = ['Wild Type', '12K Mutant', '13P Mutant', 'B7 Mutant', '11P Mutant', '11K Mutant', '7K Mutant', 'P11 Mutant',];

  $self->setMainLegend({colors => ['#996600', '#FF0000', '#FF6600','#FFFF00','#33FF66', '#009900', '#0000CC', '#660033',], short_names => $legend, cols=> 5});

  my @profileSetsArray = (['TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant', 'standard error - TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant', '']);
  my @percentileSetsArray = (['percentile - TgRH_Matrajt_GSE23174_Bz_WildType_V_Mutant', '',''],);

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
