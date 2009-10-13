package ApiCommonWebsite::View::GraphPackage::ToxoDB::White::TzBz::RMA;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::ToxoDB::White::TzBz );

use ApiCommonWebsite::View::GraphPackage::ToxoDB::White::TzBz;

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  my $_ttl  = 'expression profiles of three Tgondii strains under both normal-tachyzoite and induced-bradyzoite conditoins';

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
  $Self->setColors([ '#616D7E', '#806D7E', '#4E387E' ]);
  $Self->setTagRx(undef);

  return $Self;
}


1;
