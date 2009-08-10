
package PlasmoDBWebsite::View::GraphPackage::DeRisiProfileQuery::Ver1;

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
@ISA = qw( PlasmoDBWebsite::View::GraphPackage::DeRisiProfileQuery );

use PlasmoDBWebsite::View::GraphPackage::DeRisiProfileQuery;

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
      ProfileSet => 'DeRisi HB3 Smoothed Averaged',
    )
  );

  $Self->setQueryProfile
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => 'query',
      ProfileSet   => 'DeRisi HB3 Smoothed Averaged',
      UseSecondary => 1,
    )
  );

  return $Self;
}

# ---------------------------- End of Package ----------------------------

1;

