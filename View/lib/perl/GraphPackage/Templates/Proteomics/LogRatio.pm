package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use ApiCommonWebsite::View::GraphPackage::Util;

sub restrictProfileSetsBySourceId { return 1;}


1;

#--------------------------------------------------------------------------------

# This is an example of customizing a graph.  The template will provide things like colors (ie. we still inject stuff for it below!!
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::tbruTREU927_quantitativeMassSpec_Urbaniak_CompProt_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio );
use strict;

sub getProfileRAdjust { return 'profile.df = log2(profile.df);'}

1;



package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_aef92040c6;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $legend = [''];
#  $profile->setSampleLabels($legend);
}

1;

#package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_8e133ba94b;

#sub finalProfileAdjustments {
#  my ($self, $profile) = @_;

#  my $legend = [''];
 # $profile->setSampleLabels($legend);
#}

#1;

#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR proteomicsSimpleLogRatio
