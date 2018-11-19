package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Sage::Ver1;

=pod

=head1 Description

Provides initialization parameters for the stress data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::GiardiaDB::Sage );

use EbrcWebsiteCommon::View::GraphPackage::GiardiaDB::Sage;

use EbrcWebsiteCommon::Model::CannedQuery::Profile;
use EbrcWebsiteCommon::Model::CannedQuery::ProfileSet;
use EbrcWebsiteCommon::Model::CannedQuery::ElementNames;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $_ttl  = 'giar sage count';

	$Self->setDataQuery
	( EbrcWebsiteCommon::Model::CannedQuery::Profile->new
		( Name         => '_data',
      ProfileSet   => $_ttl,
		)
	);

	$Self->setNamesQuery
	( EbrcWebsiteCommon::Model::CannedQuery::ElementNames->new
		( Name         => '_names',
      ProfileSet   => $_ttl,
		)
	);

  $Self->setYaxisLabel('Count');
  $Self->setColors([ 'darkorange' ]);
  $Self->setTagRx(undef);

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
