package ApiCommonWebsite::View::GraphPackage::EuPathDB::WestenbergerVivax::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['black', 
                'black', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'DarkGoldenRod', 
                'black', 
                'DarkCyan',
                'DarkCyan',
                'DarkCyan',
                'DarkCyan',
               ];

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['westenberger vivax expression profile']]);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['percentile - westenberger vivax expression profile']]);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setElementNameMarginSize(7);


  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setElementNameMarginSize(7);

  $self->setGraphObjects($rma, $percentile);

  return $self;


}

1;

