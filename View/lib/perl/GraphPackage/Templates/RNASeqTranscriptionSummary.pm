package ApiCommonWebsite::View::GraphPackage::Templates::RNASeqTranscriptionSummary;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;
use LWP::Simple;
use JSON;
use Encode;

# @Override
sub getAllProfileSetNames {
  my ($self) = @_;

  my $id = $self->getId();

  my @rv = ();

  my $url = $self->getBaseUrl() . '/a/service/profileSet/TranscriptionSummaryProfiles/' . $id;
  my $content = get($url);
  my $json = from_json(Encode::decode('UTF-8', $content));
  foreach my $profile (@$json) {
    my $profileName = $profile->{'profile_set_name'};
    my $nodeType    = $profile->{'node_type'};
    my $displayName = $profile->{'display_name'};
    next if defined $profileName && $profileName =~ /[^\x00-\x7F]/;
    next if defined $displayName && $displayName =~ /[^\x00-\x7F]/;
    next if($self->isExcludedProfileSet($profileName));
    my $fullProfileName = defined $nodeType ? "$profileName [$nodeType]" : $profileName;
    my $p = {profileName=>$fullProfileName, profileType=>'values', displayName=>$displayName};
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
