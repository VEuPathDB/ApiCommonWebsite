package ApiCommonWebsite::View::GraphPackage::PlasmoDB::CortesTransVar::CrossStrain;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;

use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $legendColors = ['#FF0000','#FFFF00','#0000CC','#009900',];
  my $shortNames = ['3D7 derived strains', '7G8 derived strains', 'D10 derived strains', 'HB3 derived strains'];
  $self->setMainLegend({colors => $legendColors, short_names => $shortNames, cols => 2});


  my $strainNames=['P.f. 10g','P.f. 1,2b','P.f. 3d7b','P.f. 7g8','P.f. ab10','P.f. ab6',
                    'P.f. bb8','P.f. bc4','P.f. d10','P.f. e3','P.f. f1','P.f. g2',
                    'P.f. g4','P.f. hb3a','P.f. hb3b','P.f. kg7','P.f. ld10','P.f. w41',
                    'P.f. we5','P.f. zf8',];

  my $colorSet = [ '#FF0000', '#FF0000', '#FF0000','#FFFF00', '#009900','#009900','#009900','#009900', '#0000CC','#0000CC','#0000CC','#0000CC','#0000CC', '#009900','#009900','#FFFF00','#FFFF00', '#FF0000', '#FFFF00','#FFFF00',];
  my @colors = @$colorSet;

  my @profileSetNames = (['Profiles of transcriptional variation in Plasmodium falciparum all_strains', '' ,$strainNames]);
  my @percentileSetNames = (['red percentile - Profiles of transcriptional variation in Plasmodium falciparum all_strains','',$strainNames]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors(\@colors);
  $ratio->setElementNameMarginSize (6);


  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors(\@colors);
  $percentile->setElementNameMarginSize (6);

  $self->setGraphObjects($ratio, $percentile);

  return $self;
}



1;
