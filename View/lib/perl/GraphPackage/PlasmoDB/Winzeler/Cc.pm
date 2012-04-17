package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Cc;

use vars qw( @ISA );

use strict;

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::BarPlot;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('cyan', 'purple', 'brown' );
  my $legend = ['sorbitol', 'temperature', 'sporozoite'];

  $self->setMainLegend({colors => \@colors, short_names => $legend, cols => 4});


  my @temp_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::TemperatureTimes();
  my @sorb_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::SorbitolTimes();

  my @winzelerNames = ("S", "ER","LR", "ET", "LT","ES", "LS", "M", "G"); 

  # Want line graph for ER-LS so the element names must be numeric when they are read in
  my @tempNames = (2..7, "M");
  my @sorbNames = (2..7, "M", "G");

  my @winzelerProfileArray = (['winzeler_cc_sorbExp','', \@sorbNames],
                              ['winzeler_cc_tempExp', '', \@tempNames],
                              ['winzeler_cc_sporExp', 'standard error - winzeler_cc_sporExp', [1]]
                             );

  my @winzelerPercentileArray = (['percentile - winzeler_cc_sorbExp'],
                                 ['percentile - winzeler_cc_tempExp'],
                                 ['percentile - winzeler_cc_sporExp']
                                );

  my $winzelerProfileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@winzelerProfileArray);
  my $winzelerPercentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@winzelerPercentileArray);

  my $winzeler = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new(@_);
  $winzeler->setProfileSets($winzelerProfileSets);
  $winzeler->setColors(\@colors);
  $winzeler->setPartName('winzeler');
  $winzeler->setPointsPch([15,15,15]);
  $winzeler->setAdjustProfile('points.df = points.df - mean(points.df[points.df > 0], na.rm=T);lines.df = lines.df - mean(lines.df[lines.df > 0], na.rm=T)');
  $winzeler->setArePointsLast(1);
  $winzeler->setSampleLabels(\@winzelerNames);


  my $rma = ApiCommonWebsite::View::GraphPackage::BarPlot::RMA->new(@_);
  $rma->setProfileSets($winzelerProfileSets);
  $rma->setColors(\@colors);
  $rma->setAdjustProfile('profile.df = cbind(profile.df[,9], profile.df[,1:8]);');
  $rma->setSampleLabels(\@winzelerNames);
  $rma->setSpaceBetweenBars(1);

  my $percentile = ApiCommonWebsite::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($winzelerPercentileSets);
  $percentile->setColors(\@colors);
  $percentile->setAdjustProfile('profile.df = cbind(profile.df[,9], profile.df[,1:8]);');
  $percentile->setSampleLabels(\@winzelerNames);
  $percentile->setSpaceBetweenBars(1);

  $self->setGraphObjects($winzeler, $rma, $percentile);

  return $self;


}

1;










