package ApiCommonWebsite::View::GraphPackage::GiardiaDB::Sage;

=pod

=head1 Summary

apidb.ProfileSet "Sage count in Giardia lamblia"

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;
use vars qw( @ISA );

@ISA = qw( EbrcWebsiteCommon::View::GraphPackage::BasicBarPlot );

use EbrcWebsiteCommon::View::GraphPackage::BasicBarPlot;

#use Time::HiRes qw ( time );

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

sub init {
   my $Self = shift;
	 my $Args = ref $_[0] ? shift : {@_};

	 $Self->SUPER::init($Args);
	 $Self->{YMin} = 0;

	 return $Self;
}



1;
