
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiRaw::Dd2;

=pod

=head1 Description

Plot of deRisi raw data for red and green channels of the Dd2 strain.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiRaw );

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiRaw;
use ApiCommonWebsite::Model::CannedQuery::Profile;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

	$Self->setRedQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'red',
			ProfileSet => 'DeRisi Dd2 Raw Red repsCollapsed',
		)
	);

	$Self->setGreenQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'green',
			ProfileSet => 'DeRisi Dd2 Raw Green repsCollapsed',
		)
	);

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;

