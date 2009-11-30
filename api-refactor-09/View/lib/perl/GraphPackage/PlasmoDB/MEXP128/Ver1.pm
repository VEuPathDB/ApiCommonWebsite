
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP128::Ver1;

=pod

=head1 Description

Provides initialization parameters for the strain polymorphism data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP128 );

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP128;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $_ttl  = 'Expression profile of 3D7 clones 3D7AH1S2 and 3D7S8.4 at ring, trophozite and schizont stages.-Averaged';

	$Self->setDataQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => '_data',
      ProfileSet   => $_ttl,
		)
	);

	$Self->setDataNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => '_names',
      ProfileSet   => $_ttl,
		)
	);

  $Self->setDataYaxisLabel('M Value');
  $Self->setDataColors([ '#B22222' ]);


$_ttl  = '3D7 clones 3D7AH1S2 and 3D7S8.4 at ring, trophozite and schizont stages.- Averaged Percentiles';

	$Self->setPctQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => '_data',
      ProfileSet   => $_ttl,
		)
	);

	$Self->setPctNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => '_names',
      ProfileSet   => $_ttl,
		)
	);

  $Self->setPctYaxisLabel('percentile');
  $Self->setPctColors([ '#E6E6FA' ]);

  $Self->setTagRx(undef);

  $Self->setPctIsDecimal(1);

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
