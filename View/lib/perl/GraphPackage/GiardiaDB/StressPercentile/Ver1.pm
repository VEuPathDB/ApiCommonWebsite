package GiardiaDBWebsite::View::GraphPackage::StressPercentile::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( GiardiaDBWebsite::View::GraphPackage::StressPercentile );

use GiardiaDBWebsite::View::GraphPackage::StressPercentile;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  my $_ttl  = 'Stress Response percentiles by temperature or varying DTT concentrations-red values';

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

  $_ttl  = 'Stress Response percentiles by temperature or varying DTT concentrations-green values';

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
