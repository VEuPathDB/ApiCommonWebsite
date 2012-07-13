package ApiCommonWebsite::View::GraphPackage::PlasmoDB::CortesTransVar::TimeSeries;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('red','pink','purple',);
  my @legend = ('3D7', 'MACS-purified 3D7', 'isolate NF54');

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 3});

  $self->setPlotWidth(450);

  my @profileArray_3D7 = (['Profiles of transcriptional variation in Plasmodium falciparum 10g',],
                          ['Profiles of transcriptional variation in Plasmodium falciparum 1_2b'],
                          ['Profiles of transcriptional variation in Plasmodium falciparum 3d7a'],
                          ['Profiles of transcriptional variation in Plasmodium falciparum 3d7b'],
                          ['Profiles of transcriptional variation in Plasmodium falciparum w41'],
                         );

   my @profileArray_7G8 = (['Profiles of transcriptional variation in Plasmodium falciparum 7g8'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum kg7'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum ld10'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum we5'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum zf8'],
                          );


   my @profileArray_HB3 = (['Profiles of transcriptional variation in Plasmodium falciparum hb3a'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum hb3b'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum ab10'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum ab6'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum bb8'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum bc4'],
                          );

   my @profileArray_D10 = (['Profiles of transcriptional variation in Plasmodium falciparum d10'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum e3'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum f1'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum g2'],
                           ['Profiles of transcriptional variation in Plasmodium falciparum g4'],
                          );

  my @profileArray_parental = (['Profiles of transcriptional variation in Plasmodium falciparum 3d7a'],
                               ['Profiles of transcriptional variation in Plasmodium falciparum 7g8'],
                               ['Profiles of transcriptional variation in Plasmodium falciparum hb3a'],
                               ['Profiles of transcriptional variation in Plasmodium falciparum d10'],
                              );

  my @percentileArray_3D7 = (['red percentile - Profiles of transcriptional variation in Plasmodium falciparum 10g',],
                             ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum 1_2b'],
                             ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum 3d7a'],
                             ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum 3d7b'],
                             ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum w41'],
                            );

  my @percentileArray_7G8 = (['red percentile - Profiles of transcriptional variation in Plasmodium falciparum 7g8'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum kg7'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum ld10'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum we5'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum zf8'],
                             );

   my @percentileArray_HB3 = (['red percentile - Profiles of transcriptional variation in Plasmodium falciparum hb3a'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum hb3b'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum ab10'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum ab6'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum bb8'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum bc4'],
                             );

   my @percentileArray_D10 = (['red percentile - Profiles of transcriptional variation in Plasmodium falciparum d10'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum e3'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum f1'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum g2'],
                              ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum g4'],
                          );

  my @percentileArray_parental = (  ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum 3d7a'],
                                    ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum 7g8'],
                                    ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum hb3a'],
                                    ['red percentile - Profiles of transcriptional variation in Plasmodium falciparum d10'],
                              );



  my $profileSets_3d7 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray_3D7);
  my $profileSets_7g8 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray_7G8);
  my $profileSets_hb3 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray_HB3);
  my $profileSets_d10 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray_D10);
  my $profileSets_parental = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileArray_parental);
  my $percentileSets_3d7 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileArray_3D7);
  my $percentileSets_7g8 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileArray_7G8);
  my $percentileSets_hb3 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileArray_HB3);
  my $percentileSets_d10 = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileArray_D10);
  my $percentileSets_parental = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileArray_parental);

  my $ratio_3d7 = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $ratio_3d7->setProfileSets($profileSets_3d7);
  $ratio_3d7->setPartName('exprn_val_3d7');
  $ratio_3d7->setColors(\@colors);
  $ratio_3d7->setPointsPch([19,19,19,19,19,19]);

  my $percentile_3d7 = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile_3d7->setProfileSets($percentileSets_3d7);
  $percentile_3d7->setPartName('percentile_3d7');
  $percentile_3d7->setColors(\@colors);
  $percentile_3d7->setPointsPch([19,19,19,19,19,19]);

  my $ratio_7g8 = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $ratio_7g8->setProfileSets($profileSets_7g8);
  $ratio_7g8->setPartName('exprn_val_7g8');
  $ratio_7g8->setColors(\@colors);
  $ratio_7g8->setPointsPch([19,19,19,19,19,19]);

  my $percentile_7g8 = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile_7g8->setProfileSets($percentileSets_7g8);
  $percentile_7g8->setPartName('percentile_7g8');
  $percentile_7g8->setColors(\@colors);
  $percentile_7g8->setPointsPch([19,19,19,19,19,19]);

  my $ratio_hb3 = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $ratio_hb3->setProfileSets($profileSets_hb3);
  $ratio_hb3->setPartName('exprn_val_hb3');
  $ratio_hb3->setColors(\@colors);
  $ratio_hb3->setPointsPch([19,19,19,19,19,19]);

  my $percentile_hb3 = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile_hb3->setProfileSets($percentileSets_hb3);
  $percentile_hb3->setPartName('percentile_hb3');
  $percentile_hb3->setColors(\@colors);
  $percentile_hb3->setPointsPch([19,19,19,19,19,19]);

  my $ratio_d10 = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $ratio_d10->setProfileSets($profileSets_d10);
  $ratio_d10->setPartName('exprn_val_d10');
  $ratio_d10->setColors(\@colors);
  $ratio_d10->setPointsPch([19,19,19,19,19,19]);

  my $percentile_d10 = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile_d10->setProfileSets($percentileSets_d10);
  $percentile_d10->setPartName('percentile_d10');
  $percentile_d10->setColors(\@colors);
  $percentile_d10->setPointsPch([19,19,19,19,19,19]);

  my $ratio_parental = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $ratio_parental->setProfileSets($profileSets_parental);
  $ratio_parental->setPartName('exprn_val_parental');
  $ratio_parental->setColors(\@colors);
  $ratio_parental->setPointsPch([19,19,19,19,19,19]);

  my $percentile_parental = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile_parental->setProfileSets($percentileSets_parental);
  $percentile_parental->setPartName('percentile_parental');
  $percentile_parental->setColors(\@colors);
  $percentile_parental->setPointsPch([19,19,19,19,19,19]);

  $self->setGraphObjects($ratio_3d7, $percentile_3d7, $ratio_7g8, $percentile_7g8,
                         $ratio_hb3, $percentile_hb3, $ratio_d10, $percentile_d10,
                         $ratio_parental, $percentile_parental,);

  return $self;
}




1;



