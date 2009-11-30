
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiByOligo::HB3;

=pod

=head1 Description

Provides initiailzation parameters for a C<DeRisiByOligo> for the data
on the HB3 strain.

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
			ProfileSet => 'DeRisi HB3 Smoothed',
		)
	);

	$Self->setRoughQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'rough',
			ProfileSet => 'DeRisi HB3 Normalized repsCollapsed',
		)
	);

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

	$Self->setPercentQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'percent',
			ProfileSet => 'DeRisi HB3 Percents',
		)
	);

  $Self->setLifeStageQuery
  ( ApiCommonWebsite::Model::CannedQuery::ProfileSet->new
    ( Name       => 'lifestage',
      ProfileSet => 'DeRisi HB3 Life Stage Fractions',
    )
  );

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
