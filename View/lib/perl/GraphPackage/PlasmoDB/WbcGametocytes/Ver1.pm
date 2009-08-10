
package PlasmoDBWebsite::View::GraphPackage::WbcGametocytes::Ver1;

=pod

=head1 Description

Provides initialization parameters for the Winzeler, Baker, and
Carucci Gametocyte data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( PlasmoDBWebsite::View::GraphPackage::WbcGametocytes );

use PlasmoDBWebsite::View::GraphPackage::WbcGametocytes;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my @elementOrder = qw( 1 2 3 6 8 12 );

	$Self->setPctQuery_3d7
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => '3d7_pct',
      ProfileSet   => 'winzeler_3D7_pct',
      ScaleY       => 100,
      ElementOrder => \@elementOrder,
		)
	);

	$Self->setPctQuery_Macs3d7
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => 'MACS_3D7_pct',
			ProfileSet   => 'winzeler_3D7_MACpct',
      ScaleY       => 100,
		)
	);

	$Self->setPctQuery_NF54
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'NF54_pct',
			ProfileSet => 'winzeler_NF54_pct',
      ScaleY     => 100,
		)
	);

	$Self->setAbsQuery_3d7
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => '3d7_abs',
			ProfileSet => 'winzeler_3D7_gametocyte',
      ElementOrder => \@elementOrder,
		)
	);

	$Self->setAbsQuery_Macs3d7
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'MACS_3D7_abs',
			ProfileSet => 'winzeler_3D7_MAC',
		)
	);

	$Self->setAbsQuery_NF54
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'NF54_abs',
			ProfileSet => 'winzeler_NF54_gametocyte',
		)
	);

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
