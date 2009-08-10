
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiWinzeler::Ver1;

=pod

=head1 Description

=cut

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiWinzeler );

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiWinzeler;
use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping;

# ========================================================================
# ------------------------------- Methods --------------------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

   my @temp_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::TemperatureTimes();
   my @sorb_times = ApiCommonWebsite::View::GraphPackage::PlasmoDB::Winzeler::Mapping::SorbitolTimes();

  $Self->setDeRisi_Hb3_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name      => 'dhr',
      ProfileSet => 'DeRisi HB3 Smoothed Averaged',
    )
  );

  $Self->setDeRisi_Hb3_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name       => 'dhp',
      ProfileSet => 'DeRisi HB3 Percents Averaged',
    )
  );

  $Self->setDeRisi_3d7_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name      => 'd3r',
      ProfileSet => 'DeRisi 3D7 Smoothed Averaged',
    )
  );

  $Self->setDeRisi_3d7_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name       => 'd3p',
      ProfileSet => 'DeRisi 3D7 Percents Averaged',
    )
  );

  $Self->setDeRisi_Dd2_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name      => 'ddr',
      ProfileSet => 'DeRisi Dd2 Smoothed Averaged',
    )
  );

  $Self->setDeRisi_Dd2_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name       => 'ddp',
      ProfileSet => 'DeRisi Dd2 Percents Averaged',
    )
  );

	# Winzeler
	# ........................................

  $Self->setWinzeler_Sorb_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name        => 'wsr',
      ProfileSet  => 'winzeler_cc_sorbExp',
			#Scale       => 5,
			#Offset      => 3,
      #Floor       => 1/1000,
      ElementOrder => \@sorb_times,
    )
  );

  $Self->setWinzeler_Sorb_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name        => 'wsp',
      ProfileSet  => 'winzeler_cc_sorbPct',
			#Scale       => 5,
			#Offset      => 3,
      ElementOrder => \@sorb_times,
    )
  );

  $Self->setWinzeler_Temp_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => 'wtr',
      ProfileSet   => 'winzeler_cc_tempExp',
			#Scale        => 5,
			#Offset       => 3,
      #Floor        => 1/1000,
      ElementOrder => \@temp_times,
    )
  );

  $Self->setWinzeler_Temp_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => 'wtp',
      ProfileSet   => 'winzeler_cc_tempPct',
			#Scale        => 5,
			#Offset       => 3,
      ElementOrder => \@temp_times,
    )
  );

  return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
