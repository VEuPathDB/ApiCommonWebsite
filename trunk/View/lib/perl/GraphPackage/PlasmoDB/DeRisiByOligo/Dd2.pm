
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiByOligo::Dd2;

=pod

=head1 Description

Provides initiailzation parameters for a C<DeRisiByOligo> for the data
on the Dd2 strain.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiByOligo );

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiByOligo;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

	$Self->setSmoothQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name      => 'smooth',
			ProfileSet => 'DeRisi Dd2 Smoothed',
		)
	);

	$Self->setRoughQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'rough',
			ProfileSet => 'DeRisi Dd2 Normalized repsCollapsed',
		)
	);

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

	$Self->setPercentQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'percent',
			ProfileSet => 'DeRisi Dd2 Percents',
		)
	);

  $Self->setLifeStageQuery
  ( ApiCommonWebsite::Model::CannedQuery::ProfileSet->new
    ( Name       => 'lifestage',
      ProfileSet => 'DeRisi Dd2 Life Stage Fractions',
    )
  );

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
