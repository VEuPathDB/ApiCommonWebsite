package GiardiaDBWebsite::View::GraphPackage::Stress::Ver1;

=pod

=head1 Description

Provides initialization parameters for the stress data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( GiardiaDBWebsite::View::GraphPackage::Stress );

use GiardiaDBWebsite::View::GraphPackage::Stress;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $_ttl  = 'Stress Response in Giardia lamblia Trophozoites-Averaged';

	$Self->setDataQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => '_data',
      ProfileSet   => $_ttl,
		)
	);

	$Self->setNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => '_names',
      ProfileSet   => $_ttl,
		)
	);

  $Self->setYaxisLabel('Log2( Ratio )');
  $Self->setColors([ 'darkgreen' ]);
  $Self->setTagRx(undef);

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
