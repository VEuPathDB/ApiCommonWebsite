
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiProfileQuery::Ver1;

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
      ProfileSet => 'DeRisi HB3 Smoothed',
    )
  );

  $Self->setQueryProfile
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => 'query',
      ProfileSet   => 'DeRisi HB3 Smoothed',
      UseSecondary => 1,
    )
  );

  return $Self;
}

# ---------------------------- End of Package ----------------------------

1;

