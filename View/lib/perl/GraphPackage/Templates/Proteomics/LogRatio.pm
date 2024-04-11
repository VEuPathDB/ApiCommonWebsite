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


package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_1dec15a9c9;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  $profile->setYaxisLabel('Resistant to Sensitive ratio');

}
1;


package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_b500b22788;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $legend = ['Wild Type', 'delta csp-1'];
  $profile->setLegendLabels($legend);
}

1;


# Giardia
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_fe0732e89f;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  $profile->setYaxisLabel('Relative Abundance (log2)');

}
1;


# Host
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_74ac747eab;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

  my $legend = ['Cpar_infected_over_control'];
  my $colors = ['gray'];
  $profile->setLegendLabels($legend);
  $profile->setColors($colors);
}

1;


# toxo - tgonME49_quantitativeMassSpec_Hanggeli_SAG1_iTop3_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_bf624abaf0;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

#  my $legend = ['C18_v_WT_iTop3','C33_v_WT_iTop3','C30_v_WT_iTop3','C6_v_WT_iTop3','C30_v_C18_iTop3','C6_v_C18_iTop3','C30_v_C33_iTop3','C6_v_C33_iTop3','C33_v_C18_iTop3,''C6_v_30_iTop3'];
  my $colors = ['gray', 'gray', 'gray','gray', 'gray', 'gray','gray', 'gray', 'gray','gray',];
#  $profile->setLegendLabels($legend);
  $profile->setColors($colors);
}
1;

# toxo - tgonME49_quantitativeMassSpec_Hanggeli_4Clones_iTop3_RSRC
package ApiCommonWebsite::View::GraphPackage::Templates::Proteomics::LogRatio::DS_9ee5f5fb72;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;

#  my $legend = ['C18_v_WT_iLFQ','C33_v_WT_iLFQ','C30_v_WT_iLFQ','C6_v_WT_iLFQ','C30_v_C18_iLFQ','C6_v_C18_iLFQ','C30_v_C33_iLFQ','C6_v_C33_iLFQ','C33_v_C18_iLFQ','C6_v_C30_iLFQ'];
  my $colors = ['gray', 'gray', 'gray','gray', 'gray', 'gray','gray', 'gray', 'gray','gray',];
#  $profile->setLegendLabels($legend);
  $profile->setColors($colors);
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
