package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Ferdig::DD2_X_HB3;

use strict;
use vars qw( @ISA);

use Data::Dumper;

use ApiCommonWebsite::Model::CannedQuery::Profile;

use ApiCommonWebsite::View::GraphPackage::ScatterPlot;

@ISA = qw( ApiCommonWebsite::View::GraphPackage::ScatterPlot);

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setScreenSize(300);
  $self->setPlotWidth(500);

  my $pch = [19,24];

  my $colors = ['#E9967A', '#4682B4'];

  my $legend = ['DD2', 'HB3'];

  $self->setMainLegend({colors => $colors, short_names => $legend, points_pch => $pch});

  $self->setProfileSetsHash
    ({expr => {profiles => ['Profiles of DD2-HB3 expression from Ferdig',
                           ],
               y_axis_label => 'Expression Value',
               x_axis_label => ' ',
               default_y_max => 1,
               default_y_min => -1,
               make_y_axis_fold_incuction => 1,
               plot_title => 'Expression Values for the progeny of HB3 X DD2',
                },
     });

  $self->initColorsAndGlyphs();

  return $self;
}

sub initColorsAndGlyphs {
  my $self = shift;

  my $parentalAlleles = $self->queryParentalAlleles();

  my $legend = $self->getMainLegend();
  my $legendColors = $legend->{colors};
  my $legendPch= $legend->{points_pch};

  my $lookup = {'D' => [$legendColors->[0], $legendPch->[0]],
                'DD2' => [$legendColors->[0], $legendPch->[0]],
                'H' => [$legendColors->[1], $legendPch->[1]],
                'HB3' => [$legendColors->[1], $legendPch->[1]],
               };

  my (@colors, @pchs);

  foreach(@$parentalAlleles) {
    s/\s//;

    my $color = $lookup->{$_}->[0];
    my $pch = $lookup->{$_}->[1];

    if($color && $pch) {
      push @colors, $color;
      push @pchs, $pch
    } 
    else {
      push @colors , '#C0C0C0';
      push @pchs, 20;
    }
  }

  $self->{_profile_sets_hash}->{expr}->{colors} = \@colors;
  $self->{_profile_sets_hash}->{expr}->{points_pch} = \@pchs;
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
        ProfileSet   => 'Profiles of DD2-HB3 expression from Ferdig',
      );
  }

  my $simpleValues = $profile->getSimpleValues($_qh, {});

  my @values = map {$_->{$key}} @$simpleValues;

  return \@values;
}

1;
