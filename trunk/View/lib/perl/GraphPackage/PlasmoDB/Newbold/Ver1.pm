package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Newbold::Ver1;

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );
@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::Newbold);

use ApiCommonWebsite::View::GraphPackage;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Newbold;

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
                   ProfileSet => 'newbold gene profiles sorted mild-severe', 
		 ),
	 );

	 $Self->setMoidValuesQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'moid',
                   ProfileSet => 'newbold gene profiles sorted mild-severe',
                   Floor      => -10,
		 )
	 );
	 $Self->setPercentileQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'pct',
                   ProfileSet => 'newbold gene profiles sorted mild-severe percents',
		 )
	 );


   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
