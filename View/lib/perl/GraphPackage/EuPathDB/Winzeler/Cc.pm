package ApiCommonWebsite::View::GraphPackage::EuPathDB::Winzeler::Cc;

use vars qw( @ISA );

use strict;

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('cyan', 'purple', 'brown' );
  my $legend = ['sorbitol', 'temperature', 'sporozoite & gametocyte'];

  $self->setMainLegend({colors => \@colors, short_names => $legend, cols => 2});

  my @winzelerNames = ("S", "ER","LR", "ET", "LT","ES", "LS", "M", "G"); 

  # Want line graph for ER-LS so the element names must be numeric when they are read in
  my @tempSorbNames = (2..7, "M");

  my @winzelerProfileArray = (['winzeler_cc_sorbExp','', \@tempSorbNames],
                              ['winzeler_cc_tempExp', '', \@tempSorbNames],
                              ['winzeler_cc_sexExp', 'standard error - winzeler_cc_sexExp', [1, 'G']]
                             );

  my @winzelerPercentileArray = (['percentile - winzeler_cc_sorbExp'],
                                 ['percentile - winzeler_cc_tempExp'],
                                 ['percentile - winzeler_cc_sexExp']
                                );

  my $winzelerProfileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@winzelerProfileArray);
  my $winzelerPercentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@winzelerPercentileArray);

  my $winzeler = EbrcWebsiteCommon::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $winzeler->setProfileSets($winzelerProfileSets);
  $winzeler->setColors(\@colors);
  $winzeler->setPartName('winzeler');
  $winzeler->setPointsPch([15,15,15]);
  $winzeler->setAdjustProfile('points.df = points.df - mean(points.df[points.df > 0], na.rm=T);lines.df = lines.df - mean(lines.df[lines.df > 0], na.rm=T)');
  $winzeler->setArePointsLast(1);
  $winzeler->setSampleLabels(\@winzelerNames);


   my $rma = EbrcWebsiteCommon::View::GraphPackage::BarPlot::RMA->new(@_);
   $rma->setProfileSets($winzelerProfileSets);
   $rma->setColors(\@colors);
   $rma->addAdjustProfile('profile.df = cbind(profile.df[,8], profile.df[,1:7], profile.df[,9]);');
   $rma->setSampleLabels(\@winzelerNames);
   $rma->setSpaceBetweenBars(1);

   my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
   $percentile->setProfileSets($winzelerPercentileSets);
   $percentile->setColors(\@colors);
   $percentile->addAdjustProfile('profile.df = cbind(profile.df[,8], profile.df[,1:7], profile.df[,9]);');
   $percentile->setSampleLabels(\@winzelerNames);
   $percentile->setSpaceBetweenBars(1);

  $self->setGraphObjects($winzeler, $rma, $percentile);


  return $self;


}

1;










