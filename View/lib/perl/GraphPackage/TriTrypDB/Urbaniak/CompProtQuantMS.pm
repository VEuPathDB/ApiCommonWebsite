package ApiCommonWebsite::View::GraphPackage::TriTrypDB::Urbaniak::CompProtQuantMS;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet);
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $colors = ['#006699',];


  my @profileSetsArray = (['Profiles of tbruTREU927 comparative proteomics from Urbankiak', '', ]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $quant = ApiCommonWebsite::View::GraphPackage::BarPlot::QuantMassSpec->new(@_);
  $quant->setProfileSets($profileSets);
  $quant->setColors($colors);
  $quant->setForceHorizontalXAxis(1);


   $self->setGraphObjects($quant,);
}

1;
