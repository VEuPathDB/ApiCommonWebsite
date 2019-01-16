package ApiCommonWebsite::View::GraphPackage::AmoebaDB::Singh::SinghEhTimeSeries;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(250);
  $self->setBottomMarginSize(8);
  $self->setLegendSize(60);

  my $colors =['#E9967A', '#8B4513','#66CDAA', '#556B2F', '#87CEEB','#008080', '#C9BE62'];

  my $xAxisLabels = ['HM-1:IMSS', 'Rahman','200:NIH', '200:NIH LG','MS75-3544 1wk','MS75-3544 8wk','2592100 3wk'];

  my $legend = ["HM-1:IMSS Trophs TYI", "Rahman Trophs TYI","200:NIH Trophs TYI", "200:NIH Trophs TYI Low Glucose","Trophs/Cysts 1wk Robinsons","Trophs/Cysts 8wk Robinsons","Trophs/Cysts 3wk Robinsons"];

   $self->setMainLegend({colors => $colors, short_names => $legend,cols => 2});

  my @profileSetsArray = (['E. histolytica Gene expression in cysts and trophozoites', 'standard error - E. histolytica Gene expression in cysts and trophozoites',$xAxisLabels ]);
  my @percentileSetsArray = (['percentile - E. histolytica Gene expression in cysts and trophozoites', '', $xAxisLabels ],);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize (7);
  $rma->setScreenSize(250);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize (7);
  $percentile->setScreenSize(250);

  $self->setGraphObjects($rma, $percentile,);

  return $self;

}

1;
