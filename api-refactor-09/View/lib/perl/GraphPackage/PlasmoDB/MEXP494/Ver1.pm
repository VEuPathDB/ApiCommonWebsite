
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP494::Ver1;

=pod

=head1 Description

Provides initialization parameters for the strain polymorphism data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP494 );

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::MEXP494;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $_ttl  = 'Identification of genome wide gene copy number polymorphisms in Plasmodium falciparum';

	$Self->setDataQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => 'copyNumber_data',
      ProfileSet   => $_ttl,
		)
	);

	$Self->setNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => 'copyNumber_names',
      ProfileSet   => $_ttl,
		)
	);

  $Self->setYaxisLabel('Copy Number Polymorphisms');
  $Self->setColors([ 'red' ]);
  $Self->setTagRx('(\S+)');

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
