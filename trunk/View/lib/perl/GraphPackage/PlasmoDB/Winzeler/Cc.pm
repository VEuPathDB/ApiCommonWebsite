
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Cc;
@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler );

=pod

=head1 Summary

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler;
#use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;


# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
   my $Self = shift;

   $Self->SUPER::init(@_);

   #my @temp_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::TemperatureTimes();
   #my @sorb_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::SorbitolTimes();

	 $Self->setShortNamesQuery
	 ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		 ( Name => 'shortNames',
			 ProfileSet => 'x',
		 ),
	 );
	 $Self->setSorbitolExpressionQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name         => 'sorbExp',
			 ProfileSet   => 'winzeler_cc_sorbExp',
       Floor        => 1/1000,
       #ElementOrder => \@sorb_times,
		 )
	 );
	 $Self->setSorbitolPercentileQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name         => 'sorbPct',
			 ProfileSet   => 'winzeler_cc_sorbPct',
       #ElementOrder => \@sorb_times,
		 )
	 );
	 #$Self->setSorbitolLogPQuery
	 #( ApiCommonWebsite::Model::CannedQuery::Profile->new
	#	 ( Name         => 'sorbLgp',
	#		 ProfileSet   => 'winzeler_cc_sorbLgp',
       #ElementOrder => \@sorb_times,
	#	 )
	# );
	 $Self->setTemperatureExpressionQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'tempExp',
			 ProfileSet => 'winzeler_cc_tempExp',
       Floor      => 1/1000,
       #ElementOrder => \@temp_times,
		 )
	 );
	 $Self->setTemperaturePercentileQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name         => 'tempPct',
			 ProfileSet   => 'winzeler_cc_tempPct',
       #ElementOrder => \@temp_times,
		 )
	 );
	 #$Self->setTemperatureLogPQuery
	 #( ApiCommonWebsite::Model::CannedQuery::Profile->new
	#	 ( Name         => 'tempLgp',
	#		 ProfileSet   => 'winzeler_cc_tempLgp',
       #ElementOrder => \@temp_times,
	#	 )
	# );
	 $Self->setSporozoiteExpressionQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'sporExp',
			 ProfileSet => 'winzeler_cc_sporExp',
       #Floor      => 1/1000,
		 )
	 );
	 $Self->setSporozoitePercentileQuery
	 ( ApiCommonWebsite::Model::CannedQuery::Profile->new
		 ( Name       => 'sporPct',
			 ProfileSet => 'winzeler_cc_sporPct',
		 )
	 );
	 #$Self->setSporozoiteLogPQuery
	 #( ApiCommonWebsite::Model::CannedQuery::Profile->new
	#	 ( Name       => 'sporLgp',
	#		 ProfileSet => 'winzeler_cc_sporLgp',
	#	 )
	# );

   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
