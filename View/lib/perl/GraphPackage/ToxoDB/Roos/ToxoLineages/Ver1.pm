package ApiCommonWebsite::View::GraphPackage::ToxoDB::Roos::ToxoLineages::Ver1;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::BasicBarPlot );

use ApiCommonWebsite::View::GraphPackage::BasicBarPlot;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileSet;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;

sub init {
	my $Self = shift;

	$Self->SUPER::init(@_);

  my $name = 'Expression profiling of the 3 archetypal T. gondii lineages';

	$Self->setDataQuery
	( ApiCommonWebsite::Model::CannedQuery::Profile->new
		( Name         => 'data',
      ProfileSet   => $name,
		)
	);

	$Self->setNamesQuery
	( ApiCommonWebsite::Model::CannedQuery::ElementNames->new
		( Name         => 'names',
      ProfileSet   => $name,
		)
	);

  $Self->setYaxisLabel('Log2 expression value');
  $Self->setColors(['#4682B4', '#B22222', '#8FBC8F', '#6A5ACD', '#87CEEB', '#CD853F']);
  $Self->setYMax(10);

#  $Self->setTagRx('(.+) (rep\d)');

	return $Self;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;
