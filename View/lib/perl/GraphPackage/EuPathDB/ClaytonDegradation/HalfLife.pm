package ApiCommonWebsite::View::GraphPackage::EuPathDB::ClaytonDegradation::HalfLife;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::LinePlot;
use EbrcWebsiteCommon::View::GraphPackage::GGBarPlot;

use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  my $pch = [19,24,15,17];
  my $colors =['#996622','#0049A8',];

  my $halfLifeSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets([['mRNA decay in bloodstream and procyclic form - half_life','values', 'mRNA decay in bloodstream and procyclic form - half_life_error','values', undef,undef,undef,undef,undef]]);

  my $id = $self->getId();

  my $halfLife = EbrcWebsiteCommon::View::GraphPackage::GGBarPlot->new(@_);
  $halfLife->setProfileSets([$halfLifeSets->[0]]);
  $halfLife->setYaxisLabel('Half-life (mins)');
  $halfLife->setColors([$colors->[0],$colors->[1]]);
  $halfLife->setElementNameMarginSize(4);
  $halfLife->setPartName('half-life');
  $halfLife->setPlotTitle("Halflife - $id");

  my $rAdjustString = <<'RADJUST';
  profile.values <- profile.df.full[profile.df.full$PROFILE_SET != 'mRNA decay in bloodstream and procyclic form - half_life_error - values',]
  profile.se <- profile.df.full[profile.df.full$PROFILE_SET == 'mRNA decay in bloodstream and procyclic form - half_life_error - values',]
  names(profile.se)[names(profile.se) == 'VALUE'] <- 'STDERR'
  profile.se <- profile.se[, c('STDERR', 'NAME')]
  profile.values$STDERR <- NULL
  profile.df.full <- merge(profile.values, profile.se, by = 'NAME', all.x = TRUE)
  profile.df.full$MIN_ERR = profile.df.full$VALUE - profile.df.full$STDERR
  profile.df.full$MAX_ERR = profile.df.full$VALUE + profile.df.full$STDERR
  y.max = max(c(y.max, profile.df.full$VALUE, profile.df.full$MAX_ERR), na.rm=TRUE)
  y.min = min(c(y.min, profile.df.full$VALUE, profile.df.full$MIN_ERR), na.rm=TRUE)
RADJUST
  $halfLife->setAdjustProfile($rAdjustString);

  $self->setGraphObjects($halfLife);

  return $self;

}

1;
