
package PlasmoDBWebsite::View::GraphPackage::DeRisiRaw::HB3;

=pod

=head1 Description

Plot of deRisi raw data for red and green channels of the HB3 strain.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( PlasmoDBWebsite::View::GraphPackage::DeRisiRaw );

use PlasmoDBWebsite::View::GraphPackage::DeRisiRaw;
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
			ProfileSet => 'DeRisi HB3 Raw Red repsCollapsed',
		)
	);

	$Self->setGreenQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'green',
			ProfileSet => 'DeRisi HB3 Raw Green repsCollapsed',
		)
	);

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;

