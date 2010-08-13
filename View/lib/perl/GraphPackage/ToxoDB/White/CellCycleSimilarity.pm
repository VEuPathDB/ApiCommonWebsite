
package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::CellCycleSimilarity;

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
      ProfileSet => 'M.White Cell Cycle Microarray profiles',
    )
  );

  $Self->setQueryProfile
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => 'query',
      ProfileSet   => 'M.White Cell Cycle Microarray profiles',
      UseSecondary => 1,
    )
  );

  $Self->setYmax(10);
  $Self->setYmin(4);

  $Self->setSmoothSpline(1);
  $Self->setSplineApproxN(60);

  return $Self;
}

# ---------------------------- End of Package ----------------------------

1;

