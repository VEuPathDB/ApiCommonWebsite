package ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;
use LWP::Simple;
use JSON;

# @Override
sub getAllProfileSetNames {
  my ($self) = @_;

  my $id = $self->getId();

  my @rv = ();

  my $url = $self->getBaseUrl() . '/a/service/profileSet/TranscriptionSummaryProfiles/' . $id;
  my $content = get($url);
  my $json = from_json($content);
  foreach my $profile (@$json) {
    my $profileName = $profile->{'PROFILE_SET_NAME'};
    my $displayName = $profile->{'DISPLAY_NAME'};
    next if($self->isExcludedProfileSet($profileName));
    my $p = {profileName=>$profileName, profileType=>'values', displayName=>$displayName};
    push @rv, $p;
  }

  return \@rv;
}


# @Override
# so as not to sort plotprofiles
sub orderPlotProfiles {
  my ($self, $plotProfiles) = @_;
  return $plotProfiles;
}


1;


package ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary::All;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary );

use strict;

  sub getGraphType { 'line' }
  sub excludedProfileSetsString { '' }
  sub getSampleLabelsString { '' }
  sub getColorsString { 'black' }
  sub getForceXLabelsHorizontalString { 'true' }
  sub getBottomMarginSize { 0 }
  sub getExprPlotPartModuleString { 'RNASeqTranscriptionSummary' }
  sub getXAxisLabel { 'FPKM - Sample 1' }
  sub useLegacy { return 0; }

1;
