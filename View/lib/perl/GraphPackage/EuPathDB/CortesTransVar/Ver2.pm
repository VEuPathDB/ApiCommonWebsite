package ApiCommonWebsite::View::GraphPackage::EuPathDB::CortesTransVar::Ver2;


use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

use Data::Dumper;
sub init {
  my $self = shift;

  $self->SUPER::init(@_);
  

  my $profileBase = "Profiles of transcriptional variation in Plasmodium falciparum";
  my $parental_strains = ['3d7a','7g8','hb3a','d10'];
  my $_3d7strains = ['3d7a','10g','1_2b','3d7b','w41'];
  my $_7g8strains = ['kg7','7g8','ld10', 'we5', 'zf8'];
  my $hb3strains = ['hb3b','ab10','hb3a','ab6','bb8','bc4'];
  my $d10strains = ['e3','f1','g2','d10','g4'];


  my @colorSet = ('#FF0000','#FF6600','#FFFF00','#009900','#0000CC','#660033',);
  my @colors = (@colorSet[0..4],@colorSet[0..4],@colorSet[0..5],@colorSet[0..4],@colorSet[0..3],);
  my @legend = (@$_3d7strains);
  push(@legend, @$_7g8strains);
  push(@legend, @$hb3strains);
  push(@legend, @$d10strains);

  $self->setLegendSize(80);
  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 6});

  $self->setPlotWidth(450);
  my @parentalColors = @colorSet[0..3];
  my @_3d7Colors = @colorSet[0..4];
  my @_7g8Colors = @colorSet[0..4];
  my @hb3Colors = @colorSet[0..5];
  my @d10Colors =  @colorSet[0..4];

  my @parentalGraphs = $self->defineGraphs('Parental',$parental_strains, \@parentalColors, $profileBase, 'red percentile'); 
   my @_3d7Graphs = $self->defineGraphs('3D7 derived',$_3d7strains, \@_3d7Colors, $profileBase, 'red percentile');
   my @_7g8Graphs = $self->defineGraphs('7G8 derived',$_7g8strains, \@_7g8Colors, $profileBase, 'red percentile');
   my @hb3Graphs = $self->defineGraphs('HB3 derived',$hb3strains, \@hb3Colors, $profileBase, 'red percentile');
   my @d10Graphs = $self->defineGraphs('D10 derived',$d10strains, \@d10Colors, $profileBase, 'red percentile');

  $self->setGraphObjects(@parentalGraphs, @_3d7Graphs,  @_7g8Graphs, @hb3Graphs, @d10Graphs);

  return $self;
}

sub defineGraphs {
  my ($self, $tag, $names, $color,  $profile_base, $percentile_prefix,) = @_;
  my @pch;
  my @profileSetNames;
  my @percentileSetNames;

  foreach my $name (@$names) {
    
    my @profileSetName = ("$profile_base $name");
    my @percentileSetName = ("$percentile_prefix - $profile_base $name");
    push(@profileSetNames, [@profileSetName]);
    push(@percentileSetNames, [@percentileSetName]);
    push(@pch,15);
  }

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
  my $line = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $line->setProfileSets($profileSets);
  $line->setColors($color);
  $line->setPointsPch(\@pch);
  $line->setPartName("expr_val_$tag");
  my $lineTitle = $line->getPlotTitle();
  $line->setPlotTitle("$tag - $lineTitle");
  $line->setXaxisLabel("Hours");

  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@percentileSetNames);
  my $percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setPointsPch(\@pch);
  $percentile->setColors($color);
  $percentile->setPartName("percentile_$tag");
  my $pctTitle = $percentile->getPlotTitle();
  $percentile->setPlotTitle("$tag - $pctTitle");
  $percentile->setXaxisLabel("Hours");

  return($line, $percentile);
}
1;
