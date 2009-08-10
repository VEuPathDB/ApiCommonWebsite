package PlasmoDBWebsite::View::GraphPackage::Kappe::AveragedPercentiles;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BasicBarPlot );

use ApiCommonWebsite::View::GraphPackage::BasicBarPlot;

use Time::HiRes qw ( time );

use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

sub init {
  my $Self = shift;

  $Self->SUPER::init(@_);

  my $_ttl  = 'kappe_percents_by_condition_over_all_channels';

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
  $Self->setColors([ 'SaddleBrown' ]);
  $Self->setTagRx(undef);

  $Self->setYMin(0);

  $Self->setYScaleFactor(100);

  return $Self;
}

1;
