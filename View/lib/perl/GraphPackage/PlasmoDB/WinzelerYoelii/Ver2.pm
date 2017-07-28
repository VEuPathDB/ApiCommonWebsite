package ApiCommonWebsite::View::GraphPackage::PlasmoDB::WinzelerYoelii::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ["darkgreen", 
                "darkgreen", 
                "DarkSeaGreen",
                "DarkSeaGreen",
                "khaki", 
                "orange",
                "DarkGoldenRod",
                "DarkGoldenRod",
                "DarkGoldenRod",
                "DarkGoldenRod3",
                "DarkGoldenRod3",
                "DarkGoldenRod4",
                "DarkGoldenRod4",
               ];

  my @profileSetsArray = (['winzeler py mixed', 'standard error - winzeler py mixed']);
  my @percentileSetsArray = (['percentile - winzeler py mixed']);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize(6);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize(6);


  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;
