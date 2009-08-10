package PlasmoDBWebsite::View::GraphPackage::WinzelerYoelii::Ver1;

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );
@ISA = qw( PlasmoDBWebsite::View::GraphPackage::WinzelerYoelii);

use ApiCommonWebsite::View::GraphPackage;

use PlasmoDBWebsite::View::GraphPackage::WinzelerYoelii;

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
                   ProfileSet => 'winzeler py mixed RMAouput naturalscale', 
		 ),
	 );

	 $Self->setMoidValuesQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'moid',
                   ProfileSet => 'winzeler py mixed RMAouput naturalscale',
                   Floor      => -10,
		 )
	 );
	 $Self->setPercentileQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'pct',
                   ProfileSet => 'winzeler py mixed RMAouput naturalscale percent',
                   ScaleY => 100,
		 )
	 );


   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
