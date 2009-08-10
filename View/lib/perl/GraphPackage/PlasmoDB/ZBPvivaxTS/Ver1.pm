package PlasmoDBWebsite::View::GraphPackage::ZBPvivaxTS::Ver1;
@ISA = qw( PlasmoDBWebsite::View::GraphPackage::ZBPvivaxTS );

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

use PlasmoDBWebsite::View::GraphPackage::ZBPvivaxTS;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  my $name = 'ZB Pvivax time series normalized_averaged 1';

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

  $name = 'ZB Pvivax time series normalized_averaged 2';

  $Self->setBioRep02ExpressionQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PromastigoteTime-courseBiorep02',
        ProfileSet   => $name,
      )
    );

  $name = 'ZB Pvivax time series normalized_averaged 3';

  $Self->setBioRep03ExpressionQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PromastigoteTime-courseBiorep03',
        ProfileSet   => $name,
      )
    );

  $name = 'ZB Pvivax time series percentiles 1';

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

  $name = 'ZB Pvivax time series percentiles 2';

  $Self->setBioRep02PercentileQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PercentsPromastigoteTime-courseBiorep02',
        ProfileSet   => $name,
      )
    );

  $name = 'ZB Pvivax time series percentiles 3';

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
