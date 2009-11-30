
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisi::HB3;
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
			ProfileSet => 'DeRisi HB3 Smoothed Averaged',
		)
	);

	$Self->setRoughQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'rough',
			ProfileSet => 'DeRisi HB3 Normalized repsCollapsed Averaged',
		)
	);

	$Self->setPercentQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name       => 'percent',
			ProfileSet => 'DeRisi HB3 Percents Averaged',
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

1;
