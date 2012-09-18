package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::TzBz;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::MixedPlotSet );
use ApiCommonWebsite::View::GraphPackage::MixedPlotSet;
use ApiCommonWebsite::View::GraphPackage::LinePlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24,20,23];

  my $colors = ['#4682B4', '#6B8E23', '#00FF00', '#2E8B57'];

  my $legend = ['RH Alkaline', 'Pru Sodium Nitroprusside', 'Pru CO2-starvation', 'Pru Alkaline'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

     my @profileSetsArray = (['expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', 'standard error - expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', ''],
                             ['expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)','', ''],
                             ['expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions', '', ''],
                             ['expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions','standard error - expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions', ''],
                            );
  my @percentileSetsArray = (['percentile - expression profiles of RH delta-HXGPRT delta-UPRT strain Alkaline bradyzoite-inducing conditions', '',''],
                             ['percentile - expression profiles of Pru dHXGPRT strain Alkaline bradyzoite-inducing conditions (media pH 8.2)','',''],
                             ['percentile - expression profiles of Pru dHXGPRT strain sodium nitroprusside bradyzoite-inducing conditions','',''],
                             ['percentile - expression profiles of Pru dHXGPRT strain CO2-starvation bradyzoite-inducing conditions','',''],
                            );

  my $profileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);
  my $percentileSets = ApiCommonWebsite::View::GraphPackage::Util::makeProfileSets(\@percentileSetsArray);

  my $rma = ApiCommonWebsite::View::GraphPackage::LinePlot::RMA->new(@_);
  $rma->setProfileSets($profileSets);
  $rma->setColors($colors);
  $rma->setPointsPch($pch);

  my $percentile = ApiCommonWebsite::View::GraphPackage::LinePlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors($colors);
  $percentile->setPointsPch($pch);
  
  $self->setGraphObjects($rma, $percentile);

  return $self;
}



1;
