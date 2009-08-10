package PlasmoDBWebsite::View::GraphPackage::WestenbergerVivax::Ver1;

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );
@ISA = qw( PlasmoDBWebsite::View::GraphPackage::WestenbergerVivax);

use ApiCommonWebsite::View::GraphPackage;

use PlasmoDBWebsite::View::GraphPackage::WestenbergerVivax;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
   my $Self = shift;

   $Self->SUPER::init(@_);

	 $Self->setShortNamesQuery
	 ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		 ( Name       => 'shortNames',
                   ProfileSet => 'westenberger vivax expression profile', 
		 ),
	 );

	 $Self->setMoidValuesQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'moid',
                   ProfileSet => 'westenberger vivax expression profile',
                   Floor      => -10,
		 )
	 );
	 $Self->setPercentileQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'pct',
                   ProfileSet => 'westenberger vivax expression profile percentiles',
		 )
	 );


   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
