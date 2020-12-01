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

  my @profileSetsArray = (['TAGM-MCMC-Joint-Probability', 'sd', '', ''],
                          ['TAGM-MCMC-Joint-Probability', 'probability_mean', '', '']);

  my $profileSets = EbrcWebsiteCommon::View::GraphPackage::Util::makeProfileSets(\@profileSetsArray);

  my $cl = EbrcWebsiteCommon::View::GraphPackage::GGLinePlot::LOPIT->new(@_);
  $cl->setProfileSets($profileSets);
  $cl->setForceNoLines(1);

  my $rAdjustString = <<'RADJUST';
  profile.values <- profile.df.full[profile.df.full$PROFILE_TYPE != 'sd',]
  profile.sd <- profile.df.full[profile.df.full$PROFILE_TYPE == 'sd',]
  profile.sd$PROFILE_TYPE <- NULL
  profile.sd$PROFILE_ORDER <- NULL
  names(profile.sd)[names(profile.sd) == 'VALUE'] <- 'SD'
  profile.sd <- profile.sd[, c('SD', 'ELEMENT_NAMES')]
  profile.df.full <- merge(profile.values, profile.sd, by = 'ELEMENT_NAMES', all.x = TRUE)
  profile.df.full <- profile.df.full[order(profile.df.full$PROFILE_ORDER, profile.df.full$ELEMENT_ORDER),]
  profile.df.full$SD <- as.numeric(profile.df.full$SD)
  profile.df.full$MIN_ERR = profile.df.full$VALUE - profile.df.full$SD
  profile.df.full$MAX_ERR = profile.df.full$VALUE + profile.df.full$SD
  outlier <- profile.df.full$VALUE[profile.df.full$ELEMENT_NAMES == "outlier"]
  profile.df.full <- profile.df.full[profile.df.full$ELEMENT_NAMES != "outlier",]
RADJUST
  $cl->setAdjustProfile($rAdjustString);

  my $rPostscript = <<'RPOST';
  gp = gp + geom_errorbar(aes(ymin = MIN_ERR, ymax = MAX_ERR), colour = "black", width = .1, position = position_dodge(.9))
  gp = gp + geom_hline(aes(yintercept=outlier), colour = "red") 
  gp = gp + labs(subtitle="Red line represents outlier probability")
  gp = gp + theme(plot.subtitle = element_text(color="darkred"))
RPOST
  $cl->setRPostscript($rPostscript);

  $self->setGraphObjects($cl);

  return $self;
}



1;
