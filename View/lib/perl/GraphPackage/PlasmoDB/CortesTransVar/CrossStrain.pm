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

  my $strainNames=['P.f. 10g','P.f. 1,2b','P.f. 3d7b','P.f. 7g8','P.f. ab10','P.f. ab6',
                    'P.f. bb8','P.f. bc4','P.f. d10','P.f. e3','P.f. f1','P.f. g2',
                    'P.f. g4','P.f. hb3a','P.f. hb3b','P.f. kg7','P.f. ld10','P.f. w41',
                    'P.f. we5','P.f. zf8',];

  my $colorSet = [ '#FF0000', '#FF6600','#FFFF00','#33FF66', '#009900', '#0000CC', '#660033'];
  my @colors;
  push @colors, @$colorSet foreach (1..3);

  my @profileSetNames = (['Profiles of transcriptional variation in Plasmodium falciparum all_strains', '' ,$strainNames]);
  my @percentileSetNames = (['red percentile - Profiles of transcriptional variation in Plasmodium falciparum all_strains','',$strainNames]);

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);

  my $ratio = ApiCommonWebsite::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($profileSets);
  $ratio->setColors(\@colors);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors(\@colors);

  $self->setGraphObjects($ratio, $percentile);

  return $self;
}



1;
