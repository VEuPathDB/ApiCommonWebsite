
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisi::3D7;
@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisi );

=pod

=head1 Description

=cut

use strict;
use ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisi;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

	$Self->setSmoothQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name      => 'smooth',
			ProfileSet => 'DeRisi 3D7 Smoothed',
		)
	);

	$Self->setRoughQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'rough',
			ProfileSet => 'DeRisi 3D7 non-smoothed',
		)
	);

	$Self->setPercentQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'percent',
			ProfileSet => 'Percentiles of DeRisi 3D7 Red',
		)
	);

  $Self->setLifeStageQuery
  ( ApiCommonWebsite::Model::CannedQuery::ProfileSet->new
    ( Name       => 'lifestage',
      ProfileSet => 'DeRisi 3D7 Life Stage Fractions',
      Scale      => 52/48,
    )
  );

	return $Self;
}

1;
