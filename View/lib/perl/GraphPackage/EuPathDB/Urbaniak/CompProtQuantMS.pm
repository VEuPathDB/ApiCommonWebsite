package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Urbaniak::CompProtQuantMS;


use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#006699',];


  my @profileSetsArray = (['Profiles of tbruTREU927 comparative proteomics from Urbankiak', '', ]);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $quant = EbrcWebsiteCommon::View::GraphPackage::BarPlot::QuantMassSpec->new(@_);
  $quant->setProfileSets($profileSets);
  $quant->setColors($colors);
  $quant->setForceHorizontalXAxis(1);


   $self->setGraphObjects($quant,);
}

1;
