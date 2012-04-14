package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiWinzeler::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;
use ApiCommonWebsite::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'red', 'orange', 'cyan', 'purple' );
  my @legend = ('HB3', '3D7', 'DD2', '3D7 Sorbitol', '3D7 Temperature');

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 3});
  $self->setPlotWidth(450);

#   my $derisi = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new();
#   $derisi->setProfileSetNames(['DeRisi HB3 Smoothed',
#                                'DeRisi 3D7 Smoothed',
#                                'DeRisi Dd2 Smoothed']);
#   $derisi->setColors([@colors[0..2]]);
#   $derisi->setPlotTitle('DeRisi - log ratios');
#   $derisi->setPointsPch([15,15,15]);
#   $derisi->setPartName('derisi');



  my @temp_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::TemperatureTimes();
  my @sorb_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::SorbitolTimes();

  my @winzelerProfileArray = (['winzeler_cc_sorbExp','', \@sorb_times],
                              ['winzeler_cc_tempExp', '', \@temp_times]
                             );

  my $winzelerProfileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@winzelerProfileArray);

  my $winzeler = ApiCommonWebsite::View::GraphPackage::LinePlot::LogRatio->new();
  $winzeler->setProfileSets($winzelerProfileSets);
  $winzeler->setColors([@colors[3..4]]);
  $winzeler->setPartName('winzeler');
  $winzeler->setPointsPch([15,15]);
  $winzeler->setPlotTitle("Winzeler - log ratios");
  $winzeler->setAdjustProfile('lines.df = lines.df - mean(lines.df[lines.df > 0], na.rm=T)');

#   my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new();
#   $percentile->setProfileSetNames(['red percentile - DeRisi HB3 Smoothed',
#                                    'red percentile - DeRisi 3D7 Smoothed',
#                                    'red percentile - DeRisi Dd2 Smoothed',
#                                    'percentile - winzeler_cc_sorbExp',
#                                    'percentile - winzeler_cc_tempExp'
#                                   ]);

#   $percentile->setSampleLabels([undef,undef,undef,\@sorb_times, \@temp_times]);
#   $percentile->setColors(\@colors);
#   $percentile->setPlotTitle('Combined Expression Percentiles');
#   $percentile->setPointsPch(['NA','NA','NA','NA','NA']);

  $self->setGraphObjects($winzeler);

  return $self;
}


1;
