package ApiCommonWebsite::View::GraphPackage::PlasmoDB::CortesTransVar::TimeSeries;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

use ApiCommonWebsite::View::GraphPackage::Util;

use Data::Dumper;
sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  

  my $profileBase = 'Profiles of transcriptional variation in Plasmodium falciparum';
  my $ref_strain = ['3d7a',];
  my $parental_strains = ['7g8','hb3a','d10'];
  my $_3d7strains = ['3d7a','10g','1_2b','3d7b','w41'];
  my $_7g8strains = ['kg7','7g8','ld10', 'we5', 'zf8'];
  my $hb3strains = ['hb3b','ab10','hb3a','ab6','bb8','bc4'];
  my $d10strains = ['e3','f1','g2','d10','g4'];
  my $parental_strains = ['7g8','hb3a','d10'];

  my @colorSet = ('#FF0000','#FF6600','#FFFF00','#009900','#0000CC','#660033',);
  my @colors = (@colorSet[0..4],@colorSet[0..4],@colorSet[0..5],@colorSet[0..4],@colorSet[0..3],);
  my @legend = ("3D7 derived strains","7G8 derived strains", "HB3 derived strains", "D10 derived strains");

  my @legendColors;
  push @legendColors, 'black' for 1 .. 4;
  my @pointsPCH = (19, 24, 15, 23);
  $self->setLegendSize(80);
  $self->setMainLegend({colors => \@legendColors, short_names => \@legend, points_pch => \@pointsPCH, cols => 2, fill=> 0},);

  $self->setPlotWidth(450);
  my @refColor = ($colorSet[0],);
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
  
  my @refGraph = $self->defineGraphs('Reference_3D7-A',$ref_strain, \@refColor, $profileBase, 'red percentile', \@_3d7Pch);
  my @parentalGraphs = $self->defineGraphs('Parental',$parental_strains, \@parentalColors, $profileBase, 'red percentile', \@pointsPCH); 
  my @_3d7Graphs = $self->defineGraphs('3D7_derived',$_3d7strains, \@_3d7Colors, $profileBase, 'red percentile', \@_3d7Pch );
  my @_7g8Graphs = $self->defineGraphs('7G8_derived',$_7g8strains, \@_7g8Colors, $profileBase, 'red percentile', \@_7g8Pch );
  my @hb3Graphs = $self->defineGraphs('HB3_derived',$hb3strains, \@hb3Colors, $profileBase, 'red percentile', \@hb3Pch);
  my @d10Graphs = $self->defineGraphs('D10_derived',$d10strains, \@d10Colors, $profileBase, 'red percentile', \@d10Pch);

  $self->setGraphObjects( @refGraph, @_3d7Graphs,  @_7g8Graphs, @hb3Graphs, @d10Graphs, @parentalGraphs );

  return $self;
}

sub defineGraphs {
  my ($self, $tag, $names, $color,  $profile_base, $percentile_prefix, $pointsPch) = @_;
  my @pch;
  my @profileSetNames;
  my @percentileSetNames;
  my $bottomMargin = 6;

  foreach my $name (@$names) {

    my @profileSetName = ("$profile_base $name");
    my @percentileSetName = ("$percentile_prefix - $profile_base $name");
    push(@profileSetNames, [@profileSetName]);
    push(@percentileSetNames, [@percentileSetName]);
    
  }


   my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
   my $line = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
   $line->setProfileSets($profileSets);
   $line->setColors($color);
   $line->setPointsPch($pointsPch);
   $line->setPartName("exprn_val_$tag");
   $line->setScreenSize(250);
   $line->setElementNameMarginSize($bottomMargin);
   $line->setSmoothLines(1);
   $line->setSplineApproxN(200);
   $line->setHasExtraLegend(1);
   $line->setExtraLegendSize(6.5);
   $line->setLegendLabels($names);
   my $lineTitle = $line->getPlotTitle();
   $line->setPlotTitle("$tag - $lineTitle");
   $line->setXaxisLabel("Hours");

   my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);
   my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
   $percentile->setProfileSets($percentileSets);
   $percentile->setPointsPch($pointsPch);
   $percentile->setColors($color);
   $percentile->setPartName("percentile_$tag");
   my $pctTitle = $percentile->getPlotTitle();
   $percentile->setScreenSize(250);
   $percentile->setElementNameMarginSize($bottomMargin);
   $percentile->setSmoothLines(1);
   $percentile->setSplineApproxN(200);
   $percentile->setHasExtraLegend(1);
   $percentile->setExtraLegendSize(6.5);
   $percentile->setLegendLabels($names);
   $percentile->setPlotTitle("$tag - $pctTitle");
   $percentile->setXaxisLabel("Hours");

   return( $line, $percentile );


}
1;
