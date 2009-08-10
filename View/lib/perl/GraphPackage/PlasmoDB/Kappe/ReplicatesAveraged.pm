package PlasmoDBWebsite::View::GraphPackage::Kappe::ReplicatesAveraged;

use strict;
use vars qw( @ISA );

@ISA = qw( PlasmoDBWebsite::View::GraphPackage::Kappe );

use PlasmoDBWebsite::View::GraphPackage::Kappe;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  $Self->setColors(['blue']);

  my $_ttl  = 'kappe_fold_changes';

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

  $_ttl  = 'raw_numerators_percents';

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

  $_ttl  = 'raw_denominators_percents';

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

  $Self->setPctIsDecimal(1);

  return $Self;
}

1;
