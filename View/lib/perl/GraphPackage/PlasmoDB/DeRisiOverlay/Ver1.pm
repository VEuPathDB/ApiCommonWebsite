
package ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiOverlay::Ver1;
@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiOverlay );

=pod

=head1 Description

=cut

use strict;
use ApiCommonWebsite::View::GraphPackage::PlasmoDB::DeRisiOverlay;

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  $Self->setDeRisi_Hb3_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name      => 'dhr',
      ProfileSet => 'DeRisi HB3 Smoothed',
    )
  );

  $Self->setDeRisi_Hb3_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name       => 'dhp',
      ProfileSet => 'Percentiles of DeRisi HB3 Red',
    )
  );

  $Self->setDeRisi_3d7_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name      => 'd3r',
      ProfileSet => 'DeRisi 3D7 Smoothed',
    )
  );

  $Self->setDeRisi_3d7_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name       => 'd3p',
      ProfileSet => 'Percentiles of DeRisi 3D7 Red',
    )
  );

  $Self->setDeRisi_Dd2_Rat
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name      => 'ddr',
      ProfileSet => 'DeRisi Dd2 Smoothed',
    )
  );

  $Self->setDeRisi_Dd2_Pct
  ( ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name       => 'ddp',
      ProfileSet => 'Percentiles of DeRisi Dd2 Red',
    )
  );

  return $Self;
}

1;

