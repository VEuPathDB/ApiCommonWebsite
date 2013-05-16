package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::Quantile;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Microarray );
use ApiCommonWebsite::View::GraphPackage::Templates::Microarray;


1;




# This is an example of customizing a graph.  The template will provide things like colors (ie. we still inject stuff for it below!!
package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::Quantile::tbruTREU927_microarrayExpression_GSE17026_Matthews_Keith_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Microarray::Quantile );
use strict;

sub getProfileRAdjust { return 'points.df = points.df - lines.df[[2]]; lines.df = lines.df - lines.df[[2]];'}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setDefaultYMax(1);
  $profile->setDefaultYMin(-1);

  $profile->setMakeYAxisFoldInduction(1);

  return $profile;
}

1;


# This is an example of customizing a graph.  The template will provide things like colors (ie. we still inject stuff for it below!!
package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::Quantile::gassAWB_microarrayExpression_Troell_v1swapped_reps_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Microarray::Quantile );
use strict;

sub getProfileRAdjust { return 'points.df = points.df - lines.df[[2]]; lines.df = lines.df - lines.df[[2]];'}

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  $profile->setDefaultYMax(1);
  $profile->setDefaultYMin(-1);

  $profile->setMakeYAxisFoldInduction(1);

  return $profile;
}

1;



#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR microarraySimpleQuantileGraph
