package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use ApiCommonWebsite::View::GraphPackage::Util;


# use standard colors for all percentile graphs
sub getPercentileColors {
  return ['LightSlateGray', 'DarkSlateGray'];
}

1;


#--------------------------------------------------------------------------------


# TEMPLATE_ANCHOR proteomicsSimpleLogRatio
