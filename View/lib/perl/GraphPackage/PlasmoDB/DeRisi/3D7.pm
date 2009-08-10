
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
			ProfileSet => 'DeRisi 3D7 Smoothed Averaged',
		)
	);

	$Self->setRoughQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'rough',
			ProfileSet => 'DeRisi 3D7 Normalized repsCollapsed Averaged',
		)
	);

	$Self->setPercentQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'percent',
			ProfileSet => 'DeRisi 3D7 Percents Averaged',
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
