package ApiCommonWebsite::View::GraphPackage::CryptoDB::Kissinger::KissingerRtPcrProfiles;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;


sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors =['#E9967A', '#8B4513','#66CDAA', '#556B2F', '#87CEEB','#008080', '#C9BE62'];

  my $xAxisLabels = ['2 Hrs', '6 Hrs','12 Hrs', '24 Hrs','36 Hrs','48 Hrs','72 Hrs'];

  my @profileArray = (['Cparvum_RT_PCR_Kissinger', 'values', '', '', $xAxisLabels ]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileArray);

  my $standardized = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Standardized->new(@_);
  $standardized->setProfileSets($profileSets);
  $standardized->setColors($colors);
  $standardized->setForceHorizontalXAxis(1);

  $self->setGraphObjects($standardized);

  return $self;
}

1;
