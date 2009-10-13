package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TzBz::Percentile;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TzBz );

use ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TzBz;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  my $_ttl  = 'expression profile percentiles of T. gondii Matt_Tz-Bz time series';

  $Self->setDataQuery
    ( ApiCommonWebsite::Model::CannedQuery::Profile->new
      ( Name         => '_data',
        ProfileSet   => $_ttl,
      )
    );

  $Self->setNamesQuery
    ( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => '_names',
        ProfileSet   => $_ttl,
      )
    );


  $Self->setYaxisLabel('percentile');
  $Self->setColors([ '#7F525D','#348781',  '#A0CFEC', '#AF7817' ]);

  $Self->setTagRx(undef);

  return $Self;
}


1;
