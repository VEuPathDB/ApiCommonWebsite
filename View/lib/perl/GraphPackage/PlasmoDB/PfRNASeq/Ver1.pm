package PlasmoDBWebsite::View::GraphPackage::PfRNASeq::Ver1;
@ISA = qw( PlasmoDBWebsite::View::GraphPackage::PfRNASeq );

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

use PlasmoDBWebsite::View::GraphPackage::PfRNASeq;

# ========================================================================
# ----------------------- Create, Init, and Access -----------------------
# ========================================================================

# --------------------------------- init ---------------------------------

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  my $name = 'P.falciparum RNA Sequence Profiles';

  $Self->setExpressionNames
    ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name => 'expressionNames',
        ProfileSet => $name,
      ),
    );

  $Self->setBioRep01ExpressionQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => 'Pfalciparum_RNA_Seq',
        ProfileSet   => $name,
      )
    );

   return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
