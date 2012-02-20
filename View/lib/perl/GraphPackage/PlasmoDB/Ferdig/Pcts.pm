package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Pcts;

use strict;
use vars qw( @ISA );

use ApiCommonWebsite::Model::CannedQuery::Profile;

use Data::Dumper;

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlotSet );
use ApiCommonWebsite::View::GraphPackage::BarPlotSet;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(300);
  $self->setPlotWidth(500);

  $self->setProfileSetsHash
    ({pct => {profiles => ['Percentiles of DD2-HB3 expression from Ferdig - Red',
                           'Percentiles of DD2-HB3 expression from Ferdig - Green',
                          ],
               y_axis_label => 'Percentile',
               #x_axis_label => ' ',
               default_y_max => 50,
               colors => ['#CCCCCC','dark blue'],
             },
     });

  $self->initXAxisLabels();

  return $self;
}

sub initXAxisLabels {
  my $self = shift;

  my $xAxisLabels = $self->queryFirstProfileXAxisLabels();
  $self->{_profile_sets_hash}->{pct}->{x_axis_labels} = $xAxisLabels;
}

sub queryFirstProfileXAxisLabels {
  my ($self) = @_;

  my $_qh   = $self->getQueryHandle();

  my $profileName = $self->{_profile_sets_hash}->{pct}->{profiles}->[0];

  my ($profile, $key);

    $key = 'NAME';
    $profile = ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => "_firstProfileXAxis",
        Id           => $self->getId(),
        ProfileSet   => $profileName,
      );

  my $simpleValues = $profile->getSimpleValues($_qh, {});

  my @values = map {$_->{$key}} @$simpleValues;

  return \@values;
}

1;

