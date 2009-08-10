
package PlasmoDBWebsite::View::GraphPackage::Waters::Ver1;

=pod

=head1 Summary

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );
@ISA = qw( PlasmoDBWebsite::View::GraphPackage::Waters);

use ApiCommonWebsite::View::GraphPackage;

use PlasmoDBWebsite::View::GraphPackage::Waters;

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
			 ProfileSet => 'Waters HPE Percents Averaged', # representative from below
		 ),
	 );
	 $Self->setLogRatioHpQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'lgrHp',
			 ProfileSet => 'Waters HP Timecourse Averaged',
       Floor      => -10,
		 )
	 );
	 $Self->setPercentageHpQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'pctHp',
			 ProfileSet => 'Waters HP Percents Averaged',
       ScaleY     => 100,
		 )
	 );
	 $Self->setLogRatioHpeQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'lgrHpe',
			 ProfileSet => 'Waters HPE Timecourse Averaged',
       Floor      => -10,
		 )
	 );
	 $Self->setPercentageHpeQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'pctHpe',
			 ProfileSet => 'Waters HPE Percents Averaged',
       ScaleY     => 100,
		 )
	 );

   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
