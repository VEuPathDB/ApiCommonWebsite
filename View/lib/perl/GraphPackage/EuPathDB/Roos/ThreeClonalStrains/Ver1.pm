
package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::ThreeClonalStrains::Ver1;

=pod

=head1 Description

Provides initialization parameters for the Cowman data.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BasicBarPlot );

use EbrcWebsiteCommon::View::GraphPackage::BasicBarPlot;
use EbrcWebsiteCommon::Model::CannedQuery::Profile;
use EbrcWebsiteCommon::Model::CannedQuery::ProfileSet;
use EbrcWebsiteCommon::Model::CannedQuery::ElementNames;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $name = 'Expression profiling of the 3 archetypal T. gondii lineages';

	$Self->setDataQuery
	( EbrcWebsiteCommon::Model::CannedQuery::Profile->new
		( Name         => 'data',
      ProfileSet   => $name,
		)
	);

	$Self->setNamesQuery
	( EbrcWebsiteCommon::Model::CannedQuery::ElementNames->new
		( Name         => 'names',
      ProfileSet   => $name,
		)
	);

  $Self->setYaxisLabel('Log2 expression value');
  $Self->setColors([qw( green red blue )]);
  $Self->setTagRx('(.+) (rep\d)');

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
