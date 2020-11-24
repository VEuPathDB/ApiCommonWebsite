package ApiCommonWebsite::View::GraphPackage::EuPathDB::LOPIT::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet );
use EbrcWebsiteCommon::View::GraphPackage::MixedPlotSet;
use EbrcWebsiteCommon::View::GraphPackage::GGLinePlot;


use EbrcWebsiteCommon::View::GraphPackage::Util;

sub init {
  my $self = shift;

  $self->SUPER::init(@_);

  $self->setPlotWidth(515);

  my @profileSetsArray = (['TAGM-MCMC-Joint-Probability', 'lower_CI', '', ''],
                          ['TAGM-MCMC-Joint-Probability', 'upper_CI', '', ''],
                          ['TAGM-MCMC-Joint-Probability', 'probability_mean', '', '']);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $cl = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LOPIT->new(@_);
  $cl->setProfileSets($profileSets);
  $cl->setForceNoLines(1);

  my $rAdjustString = <<'RADJUST';
  max.err <- profile.df.full[profile.df.full$PROFILE_SET == 'TAGM-MCMC-Joint-Probability - upper_CI',]
  names(max.err)[names(max.err) == 'VALUE'] <- 'MAX_ERR'
  max.err <- max.err[, c('MAX_ERR', 'ELEMENT_NAMES')]
  min.err <- profile.df.full[profile.df.full$PROFILE_SET == 'TAGM-MCMC-Joint-Probability - lower_CI',]
  names(min.err)[names(min.err) == 'VALUE'] <- 'MIN_ERR'
  min.err <- min.err[, c('MIN_ERR', 'ELEMENT_NAMES')]
  profile.df.full <- profile.df.full[profile.df.full$PROFILE_SET == 'TAGM-MCMC-Joint-Probability - probability_mean',]
  profile.df.full <- merge(profile.df.full, min.err, by = "ELEMENT_NAMES")
  profile.df.full <- merge(profile.df.full, max.err, by = "ELEMENT_NAMES")
RADJUST
  $cl->setAdjustProfile($rAdjustString);

  my $rPostscript = <<'RPOST';
  gp = gp + geom_errorbar(aes(ymin = MIN_ERR, ymax = MAX_ERR), colour = "black", width = .1, position = position_dodge(.9)); 
RPOST
  $cl->setRPostscript($rPostscript);

  $self->setGraphObjects($cl);

  return $self;
}



1;
