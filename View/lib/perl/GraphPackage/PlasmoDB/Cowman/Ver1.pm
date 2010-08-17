
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Cowman::Ver1;

=pod

=head1 Description

Provides initialization parameters for the unlogged Cowman data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::Cowman );

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Cowman;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $invasion_ttl  = 'Profiles of Cowman Invasion KO-invasion_merozoites';
  my $knockout_ttl  = 'Profiles of Cowman Invasion KO-sir2_knockout';
  my $switching_ttl = 'Profiles of Cowman Invasion KO-molecular_mechanism_invasion';

	$Self->setInvasionDataQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => 'invasion_data',
      ProfileSet   => $invasion_ttl,
		)
	);

	$Self->setInvasionNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => 'invasion_names',
      ProfileSet   => $invasion_ttl,
		)
	);

	$Self->setSir2KoDataQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => 'sir2Ko_data',
      ProfileSet   => $knockout_ttl,
		)
	);

	$Self->setSir2KoNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => 'sir2Ko_names',
      ProfileSet   => $knockout_ttl,
		)
	);

  $Self->setPathwaysDataQuery
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => 'pathways_data',
      ProfileSet   => $switching_ttl,
    )
  );

  $Self->setPathwaysNamesQuery
  ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
    ( Name         => 'pathways_names',
      ProfileSet   => $switching_ttl,
    )
  );

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
