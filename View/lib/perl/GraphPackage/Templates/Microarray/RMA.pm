package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::RMA;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Microarray );
use ApiCommonWebsite::View::GraphPackage::Templates::Microarray;


1;


package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::RMA::linfJPCM5_microarrayExpression_Myler_promastigote_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Microarray::RMA );
use strict;


sub finalPercentileAdjustments {
  my ($self, $percentile) = @_;

  my $colors = $percentile->getColors();
  $percentile->setColors([$colors->[0]]);

  return $percentile;
}

1;





#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR microarraySimpleRmaGraph
