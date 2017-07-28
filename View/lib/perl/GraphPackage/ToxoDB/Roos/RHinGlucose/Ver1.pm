
package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::RHinGlucose::Ver1;

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
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

# ========================================================================
# --------------------------- Required Methods ---------------------------
# ========================================================================

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $name = 'Effects of Glucose Starvation in Toxoplasma gondii';

	$Self->setDataQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => 'data',
      ProfileSet   => $name,
		)
	);

	$Self->setNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => 'names',
      ProfileSet   => $name,
		)
	);

  $Self->setYaxisLabel('Log2 expression value');
  $Self->setColors( [ 'rgb(1.0,0.4,0.4)', 'rgb(0.6,0.2,0.2)' ] );
  $Self->setTagRx ( 'RH \d (.+) (rep\d)' );

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
