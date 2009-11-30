
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiRaw::3D7;

=pod

=head1 Description

Plot of deRisi raw data for red and green channels of the 3D7 strain.

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
			ProfileSet => 'DeRisi 3D7 Raw Red Arrays Collapsed',
		)
	);

	$Self->setGreenQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'green',
			ProfileSet => 'DeRisi 3D7 Raw Green Arrays Collapsed',
		)
	);

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;

