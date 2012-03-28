package ApiCommonWebsite::View::GraphPackage::PlasmoDB::ZBPvivaxTS::Ver1;
@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::ZBPvivaxTS );

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::ZBPvivaxTS;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);




#ZB Pvivax time series 1
#red percentile - ZB Pvivax time series 1
#green percentile - ZB Pvivax time series 1
#ZB Pvivax time series 2
#red percentile - ZB Pvivax time series 2
#green percentile - ZB Pvivax time series 2
#ZB Pvivax time series 3
#red percentile - ZB Pvivax time series 3
#green percentile - ZB Pvivax time series 3




  my $name = 'ZB Pvivax time series 1';

  $Self->setExpressionNames
    ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name => 'expressionNames',
        ProfileSet => $name,
      ),
    );

  $Self->setBioRep01ExpressionQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PromastigoteTime-courseBiorep01',
        ProfileSet   => $name,
      )
    );

  $name = 'ZB Pvivax time series 2';

  $Self->setBioRep02ExpressionQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PromastigoteTime-courseBiorep02',
        ProfileSet   => $name,
      )
    );

  $name = 'ZB Pvivax time series 3';

  $Self->setBioRep03ExpressionQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PromastigoteTime-courseBiorep03',
        ProfileSet   => $name,
      )
    );

  $name = 'red percentile - ZB Pvivax time series 1';

  $Self->setPercentileNames
    ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name => 'percentileNames',
        ProfileSet => $name,
      ),
    );

  $Self->setBioRep01PercentileQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PercentsPromastigoteTime-courseBiorep01',
        ProfileSet   => $name,
      )
    );

  $name = 'red percentile - ZB Pvivax time series 2';

  $Self->setBioRep02PercentileQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PercentsPromastigoteTime-courseBiorep02',
        ProfileSet   => $name,
      )
    );

  $name = 'red percentile - ZB Pvivax time series 3';

  $Self->setBioRep03PercentileQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PercentsPromastigoteTime-courseBiorep03',
        ProfileSet   => $name,
      )
    );

   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
