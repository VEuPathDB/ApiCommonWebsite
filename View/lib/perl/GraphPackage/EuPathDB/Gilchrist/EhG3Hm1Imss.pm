package ApiCommonWebsite::View::GraphPackage::AmoebaDB::Gilchrist::EhG3Hm1Imss;


use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#FFCC33', '#006699',];


  my @profileSetsArray = (['Ehist_Gilchrist_G3_V_HM1:IMSS', 'standard error - Ehist_Gilchrist_G3_V_HM1:IMSS', ]);
  my @percentileSetsArray = (['percentile - Ehist_Gilchrist_G3_V_HM1:IMSS', '',],);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setForceHorizontalXAxis(1);


  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setForceHorizontalXAxis(1);

  $self->setGraphObjects($rma, $percentile,);
}

1;
