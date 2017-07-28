package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisi::Combined;

use strict;
use vars qw( @ISA );


@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'red', 'orange');
  my @legend = ('HB3', '3D7', 'DD2');


  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 3});

  # TODO: Why isn't the scaling working??
  my $_3D7Scaling = 52/48;

  my @hb3Graphs = $self->defineGraphs('HB3', $colors[0], 'DeRisi HB3 Smoothed', 'DeRisi HB3 non-smoothed', 'red percentile - DeRisi HB3 Smoothed', 'DeRisi HB3 Life Stage Fraction', undef);
  my @_3D7Graphs = $self->defineGraphs('3D7', $colors[1], 'DeRisi 3D7 Smoothed', 'DeRisi 3D7 non-smoothed', 'red percentile - DeRisi 3D7 Smoothed', 'DeRisi 3D7 Life Stage Fraction', $_3D7Scaling);
  my @dd2Graphs = $self->defineGraphs('Dd2', $colors[2], 'DeRisi Dd2 Smoothed', 'DeRisi Dd2 non-smoothed', 'red percentile - DeRisi Dd2 Smoothed', 'DeRisi Dd2 Life Stage Fraction', undef);

  $self->setGraphObjects(@hb3Graphs, @_3D7Graphs, @dd2Graphs);

  return $self;
}

sub defineGraphs {
  my ($self, $name, $color, $smoothed, $nonSmoothed, $percentile, $fraction, $scale) = @_;

  my @pch = (15, 15);  
  my @profileSetNames = ([$smoothed],
                         [$nonSmoothed]
                        );

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);

  my $line = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $line->setProfileSets($profileSets);
  $line->setColors([$color, 'gray']);
  $line->setPointsPch(\@pch);
  $line->setPartName("expr_val_" . $name);
  my $lineTitle = $line->getPlotTitle();
  $line->setPlotTitle("$name - $lineTitle");

  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([[$percentile]]);
  my $percentile = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setPointsPch(['NA']);
  $percentile->setIsFilled(1);
  $percentile->setColors([$color]);
  $percentile->setPartName("percentile_" . $name);
  my $pctTitle = $percentile->getPlotTitle();
  $percentile->setPlotTitle("$name - $pctTitle");

  my @fractions = ([$fraction, '', '', 'Ring', $scale],
                   [$fraction, '', '', 'Schizont', $scale],
                   [$fraction, '', '', 'Trophozoite', $scale],
                  );

  my $fractionSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@fractions);

  my $postscript = "
text(8,  50, col=\"white\", labels=c(\"Ring\"));
text(23, 50, col=\"white\", labels=c(\"Trophozoite\"));
text(40, 50, col=\"white\", labels=c(\"Schizont\"));
";

  my @colors = ('#E9967A', '#4169E1', '#FF69B4');
  my $lifeStages = EbrcWebsiteCommon::View::GraphPackage::LinePlot::Filled->new(@_);
  $lifeStages->setProfileSets($fractionSets);
  $lifeStages->setPlotTitle("$name - Life Stage Population Percentages");
  $lifeStages->setYaxisLabel("%");
  $lifeStages->setColors(\@colors);
#  $lifeStages->setRPostscript($postscript);
  $lifeStages->setPointsPch(['NA', 'NA', 'NA']);
  $lifeStages->setPartName("lifeStages_" . $name);

  return($line, $percentile, $lifeStages);
}

1;










