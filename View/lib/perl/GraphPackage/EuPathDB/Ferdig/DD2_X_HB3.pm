package ApiCommonWebsite::View::GraphPackage::EuPathDB::Ferdig::DD2_X_HB3;

use strict;
use vars qw( @ISA);

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet);
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::ScatterPlot;
use EbrcWebsiteCommon::View::GraphPackage::BarPlot;
use EbrcWebsiteCommon::Model::CannedQuery::Profile;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(500);

  my $pch = [19,24];

  my $colors = ['#E9967A', '#4682B4'];

  my $legend = ['DD2', 'HB3'];

  $self->setMainLegend({colors => $colors, 
                        short_names => $legend, 
                        points_pch => $pch,
                       });

  my @profileSetNames = (['Profiles of DD2-HB3 expression from Ferdig']);
  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetNames);

  my $scatter = EbrcWebsiteCommon::View::GraphPackage::ScatterPlot::LogRatio->new(@_);
  $scatter->setProfileSets($profileSets);
  $scatter->setPlotTitle("Expression Values for the progeny of HB3 X DD2");
  $scatter->setDefaultYMax(1);
  $scatter->setDefaultYMin(-1);
  $scatter->setElementNameMarginSize(4);

  my $percentileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['red percentile - Profiles of DD2-HB3 expression from Ferdig'],
                                                                                    ['green percentile - Profiles of DD2-HB3 expression from Ferdig']]);

  my $percentile = EbrcWebsiteCommon::View::GraphPackage::BarPlot::Percentile->new(@_);
  $percentile->setProfileSets($percentileSets);
  $percentile->setColors(['#CCCCCC','dark blue']);

  $self->setGraphObjects($scatter, $percentile);

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

  my $graphObjs = $self->getGraphObjects();
  my $scatter = $graphObjs->[0];

  $scatter->setColors(\@colors);
  $scatter->setPointsPch(\@pchs);
}

sub queryParentalAlleles {
  my ($self) = @_;

  my $secondaryId = $self->getSecondaryId();

  my $_qh   = $self->getQueryHandle();

  my ($profile, $key);
  if(defined($secondaryId)) {
    $key = 'VALUES';

    $profile = EbrcWebsiteCommon::Model::CannedQuery::Profile->new
      ( Name         => "_haploblockprofile",
        Id           => $secondaryId,
        ProfileSet   => 'Haplotype profiles of P falciparum HB3-DD2 progeny - Ferdig eQTL experiment',
      );
  } 
  else {
    $key = 'NAME';
    $profile = EbrcWebsiteCommon::Model::CannedQuery::ElementNames->new
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
