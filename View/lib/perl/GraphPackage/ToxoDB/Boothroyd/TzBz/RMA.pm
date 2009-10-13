package ApiCommonWebsite::View::GraphPackage::ToxoDB::Boothroyd::TzBz::RMA;

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

  my $_ttl  = 'expression profiles of T. gondii Matt_Tz-Bz time series';

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

  $Self->setYaxisLabel('RMA Value (log2)');
  $Self->setColors([ '#7F525D','#348781',  '#A0CFEC', '#AF7817' ]);
  $Self->setTagRx(undef);

  return $Self;
}


1;
