package ApiCommonWebsite::View::GraphPackage::CryptoDB::Kissinger::RtPcrSimilarity;

=pod

=head1 Description

Grabs the smoothed averaged HB3 data for primary id (match) and
secondary id (query).

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw(@ISA);
@ISA = qw( ApiCommonWebsite::View::GraphPackage::SimilarityProfile );

use ApiCommonWebsite::View::GraphPackage::SimilarityProfile;

use ApiCommonWebsite::Model::CannedQuery::Profile;

# ========================================================================
# ------------------------------- Methods --------------------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  $Self->setMatchProfile
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name       => 'match',
      ProfileSet => 'Cparvum_RT_PCR_Kissinger',
    )
  );

  $Self->setQueryProfile
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => 'query',
      ProfileSet   => 'Cparvum_RT_PCR_Kissinger',
      UseSecondary => 1,
    )
  );

  $Self->setYmax(1);
  $Self->setYmin(-1);

#  $Self->setSmoothSpline(1);
#  $Self->setSplineApproxN(60);

  return $Self;
}

# ---------------------------- End of Package ----------------------------

1;

