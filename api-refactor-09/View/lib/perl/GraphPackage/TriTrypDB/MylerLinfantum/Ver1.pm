package ApiCommonWebsite::View::GraphPackage::TriTrypDB::MylerLinfantum::Ver1;
@ISA = qw( ApiCommonWebsite::View::GraphPackage::TriTrypDB::MylerLinfantum );

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

use ApiCommonWebsite::View::GraphPackage::TriTrypDB::MylerLinfantum;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  my $name = 'Lmajor promastigote time-course biorep01';

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

  $name = 'Lmajor promastigote time-course biorep02';

  $Self->setBioRep02ExpressionQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PromastigoteTime-courseBiorep02',
        ProfileSet   => $name,
      )
    );

  $name = 'Percents of the Lmajor promastigote time-course biorep01';


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

  $name = 'Percents of the Lmajor promastigote time-course biorep02';

  $Self->setBioRep02PercentileQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'PercentsPromastigoteTime-courseBiorep02',
        ProfileSet   => $name,
      )
    );



   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
