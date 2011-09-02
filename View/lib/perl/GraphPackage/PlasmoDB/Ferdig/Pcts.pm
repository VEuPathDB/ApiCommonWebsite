package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::Pcts;

use strict;
use vars qw( @ISA );

use ApiCommonWebsite::Model::CannedQuery::Profile;

use Data::Dumper;

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BarPlot );
use ApiCommonWebsite::View::GraphPackage::BarPlot;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(300);
  $self->setPlotWidth(500);

  my $colors = ['#E9967A', '#4682B4'];

  my $legend = ['DD2', 'HB3'];

  $self->setMainLegend({colors => $colors, short_names => $legend, cols => 1});

  $self->setProfileSetsHash
    ({pct => {profiles => ['Percentiles of DD2-HB3 expression from Ferdig - Red'],
               y_axis_label => 'Percentile',
               x_axis_label => ' ',
               default_y_max => 50,
             },
     });

  return $self;
}

sub run {
  my $self = shift;

  my $parentalAlleles = $self->queryParentalAlleles();

  my $legend = $self->getMainLegend();
  my $legendColors = $legend->{colors};

  my $lookup = {'D' => [$legendColors->[0]],
                'DD2' => [$legendColors->[0]],
                'H' => [$legendColors->[1]],
                'HB3' => [$legendColors->[1]],
               };

  my (@colors);

  foreach(@$parentalAlleles) {
    s/\s//;

    my $color = $lookup->{$_}->[0];

    if($color) {
      push @colors, $color;
    } 
    else {
      push @colors , '#C0C0C0';
    }
  }
  $self->{_profile_sets_hash}->{pct}->{colors} = \@colors;

  $self->SUPER::run(@_);
}


sub queryParentalAlleles {
  my ($self) = @_;

  my $secondaryId = $self->getSecondaryId();
  my $_qh   = $self->getQueryHandle();

  my ($profile, $key);
  if(defined($secondaryId)) {
    $key = 'VALUE';

    $profile = ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => "_haploblockprofile",
        Id           => $secondaryId,
        ProfileSet   => 'Haplotype profiles of P falciparum HB3-DD2 progeny - Ferdig eQTL experiment',
      );
  }
  else {
    $key = 'NAME';
    $profile = ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => "_haploblocknames",
        Id           => $self->getId(),
        ProfileSet   => 'Percentiles of DD2-HB3 expression from Ferdig - Red',
      );
  }

  my $simpleValues = $profile->getSimpleValues($_qh, {});

  my @values = map {$_->{$key}} @$simpleValues;

  return \@values;
}

1;

