package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(450);
  $self->setBottomMarginSize(7);

  my $roos_pch = [19,24,20,23];

  my $roos_colors = ['#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];

   my @roos_profileSetsArray = (['expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', 'standard error - expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', ''],
                             ['expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)','', ''],
                             ['expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions', '', ''],
                             ['expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions','standard error - expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions', ''],
                            );
  my @roos_percentileSetsArray = (['percentile - expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', '',''],
                             ['percentile - expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)','',''],
                             ['percentile - expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions','',''],
                             ['percentile - expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions','',''],
                            );

  my $roos_profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@roos_profileSetsArray);
  my $roos_percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@roos_percentileSetsArray);

  my $roos_rma = EbrcWebsiteCommon::View::GraphPackage::LinePlot::RMA->new(@_);
  my $id = $roos_rma->getId();  
  $roos_rma->setProfileSets($roos_profileSets);
  $roos_rma->setColors($roos_colors);
  $roos_rma->setPointsPch($roos_pch);
  $roos_rma->setPartName('roos_rma');
  $roos_rma->setPlotTitle("Percentile - $id");
  $roos_rma->setPlotTitle("0 - 72 hours : RMA Expression Value - $id");

  my $roos_percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
  $id = $roos_percentile->getId(); 
  $roos_percentile->setProfileSets($roos_percentileSets);
  $roos_percentile->setColors($roos_colors);
  $roos_percentile->setPointsPch($roos_pch);
  $roos_percentile->setPartName('roos_percentile');
  $roos_percentile->setPlotTitle("0 - 72 hours : Percentile - $id");



   my $dzierszinski_colors = ['#CD853F', '#8FBC8F'];

   my $dzierszinski_pch = [19,24];

   my @dzierszinski_profileSetsArray = (['expression profiles of VEG strain CO2-starvation bradyzoite inducing conditions : 2-6 days (by Florence Dzierszinski)','', ''],
                           ['expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions : 2-14 days (by Florence Dzierszinski)', '', '']
                           );

   my @dzierszinski_percentileSetsArray = (['percentile - expression profiles of VEG strain CO2-starvation bradyzoite inducing conditions : 2-6 days (by Florence Dzierszinski)', '',''],
                              ['percentile - expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite inducing conditions : 2-14 days (by Florence Dzierszinski)','','']);

   my $dzierszinski_profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@dzierszinski_profileSetsArray);
   my $dzierszinski_percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@dzierszinski_percentileSetsArray);

   my $dzierszinski_rma = EbrcWebsiteCommon::View::GraphPackage::LinePlot::RMA->new(@_);
   $id = $dzierszinski_rma->getId(); 
   $dzierszinski_rma->setProfileSets($dzierszinski_profileSets);
   $dzierszinski_rma->setColors($dzierszinski_colors);
   $dzierszinski_rma->setPointsPch($dzierszinski_pch);
   $dzierszinski_rma->setPartName('dzierszinski_rma');
   $dzierszinski_rma->setPlotTitle("2 - 14 days : RMA Expression Value - $id");

   my $dzierszinski_percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
   $id = $dzierszinski_percentile->getId(); 		 
   $dzierszinski_percentile->setProfileSets($dzierszinski_percentileSets);
   $dzierszinski_percentile->setColors($dzierszinski_colors);
   $dzierszinski_percentile->setPointsPch($dzierszinski_pch);
   $dzierszinski_percentile->setPartName('dzierszinski_percentile');
   $dzierszinski_percentile->setPlotTitle("2 - 14 days : Percentile - $id");


  $self->setGraphObjects($roos_rma, $roos_percentile, $dzierszinski_rma, $dzierszinski_percentile);

  return $self;
}



1;
