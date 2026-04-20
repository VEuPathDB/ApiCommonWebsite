package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  my $plotPart = $profile->getPartName();
  if ($plotPart =~/percentile/) {
    my $profileSets = $profile->getProfileSets();

    if(scalar @$profileSets > 2) {
      $profile->setFacets(["PROFILE_TYPE"]);
    }
    else {
      $profile->setHasExtraLegend(1); 
      $profile->setLegendLabels(['channel 1', 'channel 2']);
      $profile->setColors(['LightSlateGray', 'DarkSlateGray']);
    }
    
  }
}
1;


#PlasmoDB eQTL
package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_dd1931c47a;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(700);

}

sub finalProfileAdjustments {                                                                                                                                                                               
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
    tmp = profile.df.full[which(profile.df.full$ELEMENT_NAMES != 'DD2' & profile.df.full$ELEMENT_NAMES != 'HB3'),]
    tmp <- tmp[order(tmp$VALUE),]
    profile.df.full <- rbind(profile.df.full[which(profile.df.full$ELEMENT_NAMES == 'DD2' | profile.df.full$ELEMENT_NAMES == 'HB3'),], tmp)
    profile.df.full$ELEMENT_NAMES = factor(profile.df.full$ELEMENT_NAMES, levels=profile.df.full$ELEMENT_NAMES)
    profile.df.full$LEGEND = c('DD2', 'HB3', rep('Progeny', length(profile.df.full$ELEMENT_NAMES)-2))
    profile.df.full$LEGEND = factor(profile.df.full$LEGEND, levels=legend.label)
    profile.df.full$PROFILE_FILE = profile.df.full$LEGEND
    profile.is.numeric <- FALSE
RADJUST
    my $plotPart = $profile->getPartName();                                                                                                                                                                   
    if ($plotPart =~/percentile/) {
        my $profileSets = $profile->getProfileSets();

        if(scalar @$profileSets > 2) {
            $profile->setFacets(["PROFILE_TYPE"]);
        }
        else {
            $profile->setHasExtraLegend(1); 
            $profile->setLegendLabels(['channel 1', 'channel 2']);
            $profile->setColors(['LightSlateGray', 'DarkSlateGray']);
        }
    
    }
    if ($plotPart =~ /scatter/) {
        $profile->setLegendLabels(['DD2', 'HB3', 'Progeny']);
        $profile->setColors(["#FF9900","#4682B4", "#FF0000"]);
        $profile->setHideXAxisLabels(1);
        $profile->addAdjustProfile($rAdjustString);
    }
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_a4dae129e9;

sub getRemainderRegex {
  return qr/ - (.+)/;
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_0c4be69d67;

sub finalProfileAdjustments {                                                                                                                                                        
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';    
    profile.df.full$ELEMENT_NAMES = factor(profile.df.full$ELEMENT_NAMES, levels=c("bf-ld","bf-hd","0.5hr","1hr","12hr","24hr","48hr","72hr"));
    profile.df.full$GROUP = c("A","B","C","C","C","C","C","C");
RADJUST

  $profile->addAdjustProfile($rAdjustString);
 
  $profile->setXaxisLabel('');
}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_af87cb785d;

sub finalProfileAdjustments {                                                                                
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';    
    if(length(profile.df.full$NAME) == 2 ){
      profile.df.full = completeDF(profile.df.full, "STDERR");
    }
RADJUST

  $profile->addAdjustProfile($rAdjustString);

  my $plotPart = $profile->getPartName();
  if ($plotPart =~/percentile/) {
    my $profileSets = $profile->getProfileSets();

    if(scalar @$profileSets > 2) {
      $profile->setFacets(["PROFILE_TYPE"]);
    }
    else {
      $profile->setHasExtraLegend(1);
      $profile->setLegendLabels(['channel 1', 'channel 2']);
      $profile->setColors(['LightSlateGray', 'DarkSlateGray']);
    }

  }

}

1;

package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_df4f928210;

use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $bar = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::LogRatio->new(@_);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['T. brucei TbDRBD3-Depleted [microarray]','values']]);

  $bar->setProfileSets($profileSets);

  $self->setGraphObjects($bar);

  return $self;

}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_84d52f99c7;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $profileBase = 'Profiles of transcriptional variation in Plasmodium falciparum';

  my @colorSet = ('#FF0000','#FF6600','#FFFF00','#009900','#0000CC','#660033',);
  my $_3d7strains = ['3D7B','1_2B','3D7A','10G','W41'];
  my $_7g8strains = ['7G8','KG7','LD10','WE5','ZF8'];
  my $hb3strains  = ['AB6','AB10','HB3A','HB3B','BC4','BB8'];
  my $d10strains  = ['D10','E3','F1','G2','G4'];

  my @pch = (19, 24, 15, 23);
  $self->setPlotWidth(450);

  my (@_3d7Pch, @_7g8Pch, @hb3Pch, @d10Pch);
  push @_3d7Pch, $pch[0] for 1 .. 5;
  push @_7g8Pch, $pch[1] for 1 .. 5;
  push @hb3Pch,  $pch[2] for 1 .. 6;
  push @d10Pch,  $pch[3] for 1 .. 5;

  my @_3d7Graphs = $self->defineGraphs('3D7_derived', $_3d7strains, [@colorSet[0..4]], $profileBase, \@_3d7Pch);
  my @_7g8Graphs = $self->defineGraphs('7G8_derived', $_7g8strains, [@colorSet[0..4]], $profileBase, \@_7g8Pch);
  my @hb3Graphs  = $self->defineGraphs('HB3_derived', $hb3strains,  [@colorSet[0..5]], $profileBase, \@hb3Pch);
  my @d10Graphs  = $self->defineGraphs('D10_derived', $d10strains,  [@colorSet[0..4]], $profileBase, \@d10Pch);

  my $cgh_strainNames = ['10G','1_2B','3D7B','7G8','AB10','AB6','BB8','BC4','D10','E3','F1','G2','G4','HB3A','HB3B','KG7','LD10','W41','WE5','ZF8'];
  my $cgh_colorSet    = ['#FF0000','#FF0000','#FF0000','#FFFF00','#009900','#009900','#009900','#009900','#0000CC','#0000CC','#0000CC','#0000CC','#0000CC','#009900','#009900','#FFFF00','#FFFF00','#FF0000','#FFFF00','#FFFF00'];

  my $cgh_profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Cortes CGH Profiles [microarray]', 'values', '', '', $cgh_strainNames]]);

  my $ratio = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::LogRatio->new(@_);
  $ratio->setProfileSets($cgh_profileSets);
  $ratio->setColors($cgh_colorSet);
  $ratio->setElementNameMarginSize(6);
  $ratio->setYaxisLabel('Copy Number Variations (log 2)');
  $ratio->setMakeYAxisFoldInduction(0);
  $ratio->setPartName("CGH");

  $self->setGraphObjects(@_3d7Graphs, @_7g8Graphs, @hb3Graphs, @d10Graphs, $ratio);

  return $self;
}

sub defineGraphs {
  my ($self, $tag, $names, $color,  $profile_base, $pointsPch,) = @_;
  my @profileSetNames;
  my $bottomMargin = 6;

  foreach my $name (@$names) {
    $name = lc($name);
    $name =~s/,/_/;
    my @profileSetName = ("$profile_base $name [microarray]", 'values');
    push(@profileSetNames, [@profileSetName]);
    $name = uc($name);
    $name =~s/_/,/;
    $name =~s/3D7A/3D7-A/;
    $name =~s/3D7B/3D7-B/;
  }


   my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);
   my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
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


#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_28ef9b0494;

use Data::Dumper;

use EbrcWebsiteCommon::View::GraphPackage::GGPiePlot;
use LWP::Simple;
use JSON;

# @Override
sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'red', 'orange');
  my @legend = ('HB3', '3D7', 'DD2');

  # TODO: Why isn't the scaling working??
  my $_3D7Scaling = 52/48;

  my @hb3Graphs = $self->defineGraphs('HB3', $colors[0], 'DeRisi HB3 Smoothed [microarray]', 'DeRisi HB3 non-smoothed [microarray]', 'Timepoint Mapping And Life Stage Fractions - HB3 [microarray]', undef);
  my @_3D7Graphs = $self->defineGraphs('3D7', $colors[1], 'DeRisi 3D7 Smoothed [microarray]', 'DeRisi 3D7 non-smoothed [microarray]', 'Timepoint Mapping And Life Stage Fractions - 3D7 [microarray]', $_3D7Scaling);
  my @dd2Graphs = $self->defineGraphs('Dd2', $colors[2], 'DeRisi Dd2 Smoothed [microarray]', 'DeRisi Dd2 non-smoothed [microarray]', 'Timepoint Mapping And Life Stage Fractions - Dd2 [microarray]', undef);


  my $combined = $self->makeCombinedGraph();

  my @pieProfileSetNames = (['DeRisi HB3 Smoothed [microarray]', 'values']);

  my $pieProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@pieProfileSetNames);

  my $hb3Pie = EbrcWebsiteCommon::View::GraphPackage::GGPiePlot->new(@_);
  $hb3Pie->setProfileSets($pieProfileSets);
  $hb3Pie->setPartName("expr_val_pie_HB3");
  my $pieTitle = $hb3Pie->getPlotTitle();
  $hb3Pie->setPlotTitle("HB3 - $pieTitle");
  $hb3Pie->setXaxisLabel('');
  $hb3Pie->setIsDonut(1);

  my $scalingFactor = $self->getScalingFactor();

  my $size = 16 * $scalingFactor;
  if($self->getCompact()) {
    $size = 4;
  }

  $hb3Pie->addAdjustProfile("profile.df.full <- profile.df.full[!is.na(profile.df.full\$VALUE),]");

  $hb3Pie->setRPostscript("gp = gp + annotate(\"text\", x = .5, y = .5, label = profile.df.full\$ELEMENT_NAMES_NUMERIC[profile.df.full\$VALUE == max(profile.df.full\$VALUE)][1], size = $size)");

  $self->setGraphObjects($combined, @hb3Graphs, @_3D7Graphs, @dd2Graphs, $hb3Pie);

  return $self;
}

sub getTimePointMapping {
  my ($self, $timePointProfileSetName) = @_;  

  my $url = $self->getBaseUrl() . '/a/service/profileSet/TimePointMapping/' . $timePointProfileSetName;
  my $content = get($url);
  my $json = from_json($content);
  my $profileAsString = @$json[0]->{'profile_as_string'};

  my @rv = split(/\t/, $profileAsString);


  return \@rv;
}


sub makeCombinedGraph {
  my ($self) = @_;

  my $_3d7ProfileSet = 'DeRisi 3D7 Smoothed [microarray]';
  my $hb3ProfileSet = 'DeRisi HB3 Smoothed [microarray]';
  my $dd2ProfileSet = 'DeRisi Dd2 Smoothed [microarray]';

  my $times_3d7 = $self->getTimePointMapping('Timepoint Mapping And Life Stage Fractions - 3D7');
  my $times_hb3 = $self->getTimePointMapping('Timepoint Mapping And Life Stage Fractions - HB3');
  my $times_dd2 = $self->getTimePointMapping('Timepoint Mapping And Life Stage Fractions - Dd2');


  my @derisiProfileArray = ([$hb3ProfileSet, 'values', '', '', $times_hb3],
                            [$_3d7ProfileSet, 'values', '', '', $times_3d7],
                            [$dd2ProfileSet, 'values', '', '', $times_dd2],
                           );

  my $derisiProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@derisiProfileArray);

  my @colors = ('blue', 'red', 'orange', 'cyan', 'purple' );

  my $derisi = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $derisi->setProfileSets($derisiProfileSets);
  $derisi->setColors([@colors[0..2]]);
  $derisi->setPointsPch([15,15,15]);
  $derisi->setPartName('exprn_val_overlay');


  $derisi->setHasExtraLegend(1);
  $derisi->setLegendLabels(['HB3', '3D7', 'DD2']);
  $derisi->setXaxisLabel('');


  return $derisi;
}


sub defineGraphs {
  my ($self, $name, $color, $smoothed, $nonSmoothed, $fraction, $scale) = @_;

  my @pch = (15, 15);  
  my @profileSetNames = ([$smoothed, 'values'],
                         [$nonSmoothed, 'values']
                        );


  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);

  my $line = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $line->setProfileSets($profileSets);
  $line->setColors([$color, 'gray']);
  $line->setPointsPch(\@pch);
  $line->setPartName("expr_val_" . $name);
  my $lineTitle = $line->getPlotTitle();
  $line->setPlotTitle("$name - $lineTitle");
  $line->setXaxisLabel('');

   my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([[$smoothed, 'channel1_percentiles']]);
   my $percentile = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::Percentile->new(@_);
   $percentile->setProfileSets($percentileSets);
   $percentile->setPointsPch(['NA']);
   $percentile->setIsFilled(1);
   $percentile->setColors([$color]);
   $percentile->setPartName("percentile_" . $name);
   my $pctTitle = $percentile->getPlotTitle();
   $percentile->setPlotTitle("$name - $pctTitle");
   $percentile->setFillBelowLine('TRUE');
   $percentile->setXaxisLabel('');

   my $fractionSets = EbrcWebsiteCommon::View::GraphPackage::ProfileSet->new("DUMMY");
   $fractionSets->setJsonForService("{\"profileSetName\":\"$fraction\",\"profileType\":\"value\",\"idOverride\":\"erythrocytic ring trophozoite stage\",\"name\":\"erythrocytic ring trophozoite stage\"},{\"profileSetName\":\"$fraction\",\"profileType\":\"value\",\"idOverride\":\"schizont stage\",\"name\":\"schizont stage\"},{\"profileSetName\":\"$fraction\",\"profileType\":\"value\",\"idOverride\":\"trophozoite stage\",\"name\":\"trophozoite stage\"}");
   $fractionSets->setSqlName("Profile");

   my @colors = ('#E9967A', '#4169E1', '#FF69B4');
   my $lifeStages = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::Filled->new(@_);
   $lifeStages->setProfileSets([$fractionSets]);
   $lifeStages->setPlotTitle("$name - Life Stage Population Percentages");
   $lifeStages->setYaxisLabel("%");
   $lifeStages->setColors(\@colors);
   $lifeStages->setPointsPch(['NA', 'NA', 'NA']);
   $lifeStages->setPartName("lifeStages_" . $name);
   $lifeStages->setFillBelowLine('TRUE');
   $lifeStages->setLegendLabels(["Ring", "Schizont", "Trophozoite"]);
   $lifeStages->addAdjustProfile('profile.df.full$PROFILE_SET <- profile.df.full$DISPLAY_NAME');
   $lifeStages->setXaxisLabel('');

  return($line, $percentile, $lifeStages);
}

sub declareParts {
  my ($self) = @_;

  my $arrayRef = $self->SUPER::declareParts();

  my @newParts;
  foreach my $plotPart (@{$arrayRef}) {
    if ($plotPart->{visible_part} ne 'expr_val_pie_HB3') {
      push @newParts, $plotPart;
    }
  }

  return \@newParts;
}

#--------------------------------------------------------------------------------

package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_b7cf547d33;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);
 my $colors = ['blue', '#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];
  my $pch = [15,24,20,23,25];

  my $legend = ['Cln/Clb', 'pheromone', 'elutriation', 'cdc15', 'Cho et al'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch=> $pch, , cols => 3});


  my $clnClbProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Cln/Clb experiments','values']]);

  my $clnClbPlot = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot::LogRatio->new(@_);
  $clnClbPlot->setProfileSets($clnClbProfileSets);
  $clnClbPlot->setPartName('Cln_Clb');
  $clnClbPlot->setForceHorizontalXAxis(1);


  my $pheromoneProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['pheromone experiments','values']]);
  my $pheromonePlot = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $pheromonePlot->setProfileSets($pheromoneProfileSets);
  $pheromonePlot->setXaxisLabel('');
  $pheromonePlot->setPartName('pheromone');

  my $elutriationProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['elutriation experiments','values']]);
  my $elutriationPlot = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $elutriationPlot->setProfileSets($elutriationProfileSets);
  $elutriationPlot->setXaxisLabel('');
  $elutriationPlot->setPartName('elutriation');
  
  my $cdc15ProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['cdc15 experiments','values']]);
  my $cdc15Plot = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $cdc15Plot->setProfileSets($cdc15ProfileSets);
  $cdc15Plot->setPartName('cdc15');
  $cdc15Plot->setXaxisLabel('');
  $cdc15Plot->setRemoveNaN('TRUE');

  my $cdc28ProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['Cho et al','values']]);
  my $cdc28Plot = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LogRatio->new(@_);
  $cdc28Plot->setProfileSets($cdc28ProfileSets);
  $cdc28Plot->setPartName('cdc28');
  $cdc28Plot->setXaxisLabel('');
  $cdc28Plot->setRemoveNaN('TRUE');

  $self->setGraphObjects($clnClbPlot, $pheromonePlot, $elutriationPlot, $cdc15Plot, $cdc28Plot);

  return $self;
}


package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_9ec3204249;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $rAdjustString = << 'RADJUST';
    profile.df.full$ELEMENT_NAMES = factor(profile.df.full$ELEMENT_NAMES,   levels=c('Ex-vivo ring - 4hr','Ex-vivo late ring - 8hr','Ex-vivo mid trophozoite - 12hr','Ex-vivo mid trophozoite - 16hr','Ex-vivo schizont - 20hr','Ex-vivo schizont - 24hr','In-vitro I - 0hr','In-vitro I - 4hr','In-vitro I - 8hr','In-vitro I - 12hr','In-vitro I - 16hr','In-vitro I - 20hr','In-vitro I - 24hr','In-vitro I - 28hr','In-vitro II - 0hr','In-vitro II - 4hr','In-vitro II - 8hr','In-vitro II - 12hr','In-vitro II - 16hr','In-vitro II - 20hr','In-vitro II - 24hr'));

    profile.df.full$GROUP = c("A","A","A","A","A","A","B","B","B","B","B","B","B","B","C","C","C","C","C","C","C");
    profile.df.full$ELEMENT_NAMES_NUMERIC = profile.df.full$ELEMENT_NAMES;
RADJUST
    my $plotPart = $profile->getPartName();
    if ($plotPart =~/percentile/) {
        my $profileSets = $profile->getProfileSets();
	$profile->setHasExtraLegend(1); 
	$profile->setLegendLabels(['channel 1', 'channel 2']);
	$profile->setColors(['LightSlateGray', 'DarkSlateGray']);
    } else {
      $profile->addAdjustProfile($rAdjustString);
    }
 $profile->setHideXAxisLabels(1);
}
1;

# plasmo - pyoeyoelii17X_microarrayExpression_Kappe_LiverStage_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel::DS_3021e25a77;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  $self->setPlotWidth(1200);

  return $self;
}
1;

#--------------------------------------------------------------------------------
# TEMPLATE_ANCHOR microarraySimpleTwoChannelGraph




