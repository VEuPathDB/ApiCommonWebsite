package ApiCommonWebsite::View::GraphPackage::PlasmoDB::CortesTransVar::TimeSeries;


use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

use Data::Dumper;
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $profileBase = 'Profiles of transcriptional variation in Plasmodium falciparum';

  my @colorSet = ('#FF0000','#FF6600','#FFFF00','#009900','#0000CC','#660033',);
  my $_3d7strains = ['3D7A','10G','1_2B','3D7B','W41'];
  my $_7g8strains = ['KG7','7G8','LD10', 'WE5', 'ZF8'];
  my $hb3strains = ['HB3B','AB10','HB3A','AB6','BB8','BC4'];
  my $d10strains = ['E3','F1','G2','D10','G4'];
  my $parent_strains = ['3D7','7G8','HB3A','D10'];
  my @parental_strains = @$parent_strains[1..3];

 #  my $timeProfileBase = 'Cortes Transcript Variantome ';
 #  my $time = ['Hour 15','Hour 19','Hour 23','Hour 27','Hour 31','Hour 35'];

 #  my $timeProfileSet = [];
 #  my $percentileTimeProfileSet =  [];
 #  my @timeGraphs; 

 #  foreach my $strain (@$parent_strains)
 #    {
 #      foreach my $timePoint (@$time)
 #        {
 #          my $psStrain = $strain;
 #          $psStrain=~s\$strain\$strain Derived\ unless $strain=~m\HB3\;
 #          $psStrain=~s\HB3A\HB3\;
 #          $psStrain=~s\3D7\- 3D7\;
 #          my $name = $timeProfileBase.$psStrain.' : '.$timePoint;
 #          my $percentileName = 'percentile - '.$timeProfileBase.$psStrain.' : '.$timePoint;
 #          my @profileSetNames = ([$name, '' ,$timePoint]);
 #          my @pctProfileSetNames = ([$percentileName, '' ,$timePoint]);
 #          $strain=~s/ - //;
 #          my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
 #          my $pctProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@pctProfileSetNames);
 #          $timePoint =~s/ /_/;
 #          my $ratio = EbrcWebsiteCommon::View::GraphPackage::BarPlot::LogRatio->new(@_);
 #          $ratio->setProfileSets($profileSets);
 #          $ratio->setColors(\@colorSet);
 #          $ratio->setElementNameMarginSize (6);
 #          $ratio->setMakeYAxisFoldInduction(0);
 #          my $Title = $ratio->getPlotTitle();
 #          $ratio->setPlotTitle("$strain"."_Derived_$timePoint - $Title");
 #          $ratio->setPartName("exprn_val_$strain"."_Derived_$timePoint");


 #          my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
 #          $percentile->setProfileSets($pctProfileSets);
 #          $percentile->setColors(\@colorSet);
 #          $percentile->setElementNameMarginSize (6);
 #          $percentile->setMakeYAxisFoldInduction(0);
 #          $Title = $percentile->getPlotTitle();
 #          $percentile ->setPlotTitle("$strain"."_Derived_$timePoint - $Title");
 #          $percentile->setPartName("percentile_$strain"."_Derived_$timePoint");
 #          $timePoint =~s/_/ /;
 #          push(@timeGraphs,$ratio);
 #          push(@timeGraphs,$percentile);
 #        }
 #    }

 # # print STDERR Dumper(@timeGraphs );

  my @colors = (@colorSet[0..4],@colorSet[0..4],@colorSet[0..5],@colorSet[0..4],@colorSet[0..3],);
  my @legend = ("3D7 derived strains","7G8 derived strains", "HB3 derived strains", "D10 derived strains");

  my @legendColors;
  push @legendColors, 'black' for 1 .. 4;
  my @pointsPCH = (19, 24, 15, 23);
  $self->setLegendSize(80);
  $self->setMainLegend({colors => \@legendColors, short_names => \@legend, points_pch => \@pointsPCH, cols => 2, fill=> 0},);

  $self->setPlotWidth(450);
  my @_3d7Colors = @colorSet[0..4];
  my @_7g8Colors = @colorSet[0..4];
  my @hb3Colors = @colorSet[0..5];
  my @d10Colors =  @colorSet[0..4];
  my @parentalColors = @colorSet[1..3];

  my @_3d7Pch;
  my @_7g8Pch;
  my @hb3Pch;
  my @d10Pch;

  push @_3d7Pch, $pointsPCH[0] for 1 .. 5;
  push @_7g8Pch, $pointsPCH[1] for 1 .. 5;
  push @hb3Pch, $pointsPCH[2] for 1 .. 6;
  push @d10Pch, $pointsPCH[3] for 1 .. 5;

  my @parentalPch =@pointsPCH[1..3];

  my @parentalGraphs = $self->defineGraphs('Parental',\@parental_strains, \@parentalColors, $profileBase, \@parentalPch); 
  my @_3d7Graphs = $self->defineGraphs('3D7_derived',$_3d7strains, \@_3d7Colors, $profileBase,  \@_3d7Pch );
  my @_7g8Graphs = $self->defineGraphs('7G8_derived',$_7g8strains, \@_7g8Colors, $profileBase,  \@_7g8Pch );
  my @hb3Graphs = $self->defineGraphs('HB3_derived',$hb3strains, \@hb3Colors, $profileBase,  \@hb3Pch);
  my @d10Graphs = $self->defineGraphs('D10_derived',$d10strains, \@d10Colors, $profileBase, \@d10Pch);

  my $cgh_shortNames = ['3D7 derived strains', '7G8 derived strains', 'D10 derived strains', 'HB3 derived strains'];

  my $cgh_strainNames=['P.f. 10G','P.f. 1,2B','P.f. 3D7-B','P.f. 7G8','P.f. AB10','P.f. AB6',
                    'P.f. BB8','P.f. BC4','P.f. D10','P.f. E3','P.f. F1','P.f. G2',
                    'P.f. G4','P.f. HB3A','P.f. HB3B','P.f. KG7','P.f. LD10','P.f. W41',
                    'P.f. WE5','P.f. ZF8',];

  my $cgh_colorSet = [ '#FF0000', '#FF0000', '#FF0000','#FFFF00', '#009900','#009900','#009900','#009900', '#0000CC','#0000CC','#0000CC','#0000CC','#0000CC', '#009900','#009900','#FFFF00','#FFFF00', '#FF0000', '#FFFF00','#FFFF00',];
  my @cgh_colors = @$cgh_colorSet;

  my @cgh_profileSetNames = (['Cortes CGH Profiles', '' ,$cgh_strainNames]);

  my $cgh_profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@cgh_profileSetNames);


  my $ratio = EbrcWebsiteCommon::View::GraphPackage::BarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($cgh_profileSets);
  $ratio->setColors(\@cgh_colors);
  $ratio->setElementNameMarginSize (6);
  $ratio->setYaxisLabel('Copy Number Variations (log 2)');
  $ratio->setMakeYAxisFoldInduction(0);
  $ratio->setPartName("CGH");

  $self->setGraphObjects( @_3d7Graphs,  @_7g8Graphs, @hb3Graphs, @d10Graphs, @parentalGraphs, $ratio, );

  return $self;
}

sub defineGraphs {
  my ($self, $tag, $names, $color,  $profile_base, $pointsPch,) = @_;
  my @profileSetNames;
  my $bottomMargin = 6;

  foreach my $name (@$names) {
    $name = lc($name);
    $name =~s/,/_/;
    my @profileSetName = ("$profile_base $name");
    push(@profileSetNames, [@profileSetName]);
    $name = uc($name);
    $name =~s/_/,/;
    $name =~s/3D7A/3D7-A/;
    $name =~s/3D7B/3D7-B/;
  }


   my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
   my $line = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
   $line->setProfileSets($profileSets);
   $line->setColors($color);
   $line->setPointsPch($pointsPch);
   $line->setPartName("exprn_val_$tag");
   $line->setScreenSize(250);
   $line->setElementNameMarginSize($bottomMargin);
   $line->setSmoothLines(1);
   $line->setSplineApproxN(200);
   $line->setSplineDF(5);
   $line->setHasExtraLegend(1);
   $line->setExtraLegendSize(7);
   $line->setLegendLabels($names);
   my $lineTitle = $line->getPlotTitle();
   $line->setPlotTitle("$tag - $lineTitle");
   $line->setXaxisLabel("Hours");


   return( $line );



}
1;
