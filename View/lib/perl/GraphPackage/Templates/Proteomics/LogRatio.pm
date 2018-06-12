package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use EbrcWebsiteCommon::View::GraphPackage::Util;

sub restrictProfileSetsBySourceId { return 0;}


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

  my $legend = ['Esmeraldo-like', 'Non-Esmeraldo-like', ''];
  my $colors = ['gray', 'blue', 'green'];
  $profile->setLegendLabels($legend);
  $profile->setColors($colors);
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_d254e99026;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $legend = ['Esmeraldo-like', 'Non-Esmeraldo-like', ''];
  $profile->setLegendLabels($legend);
}

1;


package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_65941738a5;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my %hash = ("PGAL7::PKA1 + glucose" => "PGAL7::PKA1 + GLU",
              "PGAL7::PKA1 + galactose" => "PGAL7::PKA1 + GAL"
      );

  $self->mapSampleLabels($profile, \%hash);
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
