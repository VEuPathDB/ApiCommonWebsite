package ApiCommonWebsite::View::GraphPackage::PlasmoDB::Kappe::ReplicatesAveraged;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::PlasmoDB::Kappe );

use ApiCommonWebsite::View::GraphPackage::PlasmoDB::Kappe;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  $Self->setColors(['blue']);

  my $_ttl  = 'kappe_all_comparisons_profiles';

  $Self->setDataQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => '_data',
        ProfileSet   => $_ttl,
      )
    );

  $Self->setDataNamesQuery
    ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => '_names',
        ProfileSet   => $_ttl,
      )
    );

  $Self->setDataYaxisLabel('M value');

  $_ttl  = 'kappe_all_comparisons_percentiles_red';

  $Self->setNumQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => '_data',
        ProfileSet   => $_ttl,
      )
    );

  $Self->setNumNamesQuery
    ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => '_names',
        ProfileSet   => $_ttl,
      )
	);

  $_ttl  = 'kappe_all_comparisons_percentiles_green';

  $Self->setDenQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => '_data',
        ProfileSet   => $_ttl,
      )
    );

  $Self->setDenNamesQuery
    ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => '_names',
        ProfileSet   => $_ttl,
      )
    );

  $Self->setPctYaxisLabel('percentile');

  $Self->setPctIsDecimal(0);

  return $Self;
}

1;
