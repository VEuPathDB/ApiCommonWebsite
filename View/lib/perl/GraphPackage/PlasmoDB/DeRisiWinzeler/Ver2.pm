package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiWinzeler::Ver2;

use strict;
use vars qw( @ISA );


@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;

use Data::Dumper;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my @colors = ('blue', 'red', 'orange', 'cyan', 'purple' );
  my @legend = ('HB3', '3D7', 'DD2', '3D7 Sorbitol', '3D7 Temperature');

  $self->setMainLegend({colors => \@colors, short_names => \@legend, cols => 3});

  $self->setPlotWidth(450);

  my $derisi = ApiCommonWebsite::View::GraphPackage::LinePlot->new();
  $derisi->setProfileSetNames(['DeRisi HB3 Smoothed',
                               'DeRisi 3D7 Smoothed',
                               'DeRisi Dd2 Smoothed']);
  $derisi->setColors([@colors[0..2]]);
  $derisi->setPartName('derisi');
  $derisi->setPointsPch([15,15,15]);
  $derisi->setPlotTitle("DeRisi - log ratios");
  $derisi->setYaxisLabel("lg(Cy5/Cy3)");

  my $winzeler = ApiCommonWebsite::View::GraphPackage::LinePlot->new();
  $winzeler->setProfileSetNames(['winzeler_cc_sorbExp',
                                 'winzeler_cc_tempExp'
                              ]);

  my @temp_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::TemperatureTimes();
  my @sorb_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::SorbitolTimes();

  $winzeler->setSampleLabels([\@sorb_times, \@temp_times]);

  $winzeler->setColors([@colors[3..4]]);
  $winzeler->setPartName('winzeler');
  $winzeler->setPointsPch([15,15]);
  $winzeler->setPlotTitle("Winzeler - log ratios");
  $winzeler->setYaxisLabel("lg(Exp/Avg)");
  $winzeler->setAdjustProfile('profile = profile - mean(profile[profile > 0])');

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot->new();
  $percentile->setProfileSetNames(['red percentile - DeRisi HB3 Smoothed',
                                   'red percentile - DeRisi 3D7 Smoothed',
                                   'red percentile - DeRisi Dd2 Smoothed',
                                   'percentile - winzeler_cc_sorbExp',
                                   'percentile - winzeler_cc_tempExp'
                                  ]);

  $percentile->setSampleLabels([undef,undef,undef,\@sorb_times, \@temp_times]);
  $percentile->setPartName('percentile');
  $percentile->setColors(\@colors);
  $percentile->setPlotTitle('Combined Expression Percentiles');
  $percentile->setPointsPch(['NA','NA','NA','NA','NA']);
  $percentile->setYaxisLabel("%");

  $self->setGraphObjects($derisi, $winzeler, $percentile);

  return $self;
}


1;
