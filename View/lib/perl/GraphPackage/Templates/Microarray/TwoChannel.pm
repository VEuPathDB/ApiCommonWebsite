package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::TwoChannel;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;

use ApiCommonWebsite::View::GraphPackage::Util;


# use standard colors for all percentile graphs
sub getPercentileColors {
  return ['LightSlateGray', 'DarkSlateGray'];
}

# @Override.  Two channel microarray percentiles should be bar plots
sub getPercentileGraphType {
  return 'bar';
}

1;

#--------------------------------------------------------------------------------


# This is an example of customizing a graph.  The template will provide things like colors (ie. we still inject stuff for it below!!
package ApiCommonWebsite::View::GraphPackage::Templates::Microarray::TwoChannel::tbruTREU927_microarrayExpression_EMEXP2026_DHH1_mutant_pLEW100_24H_RSRC;
use base qw( ApiCommonWebsite::View::GraphPackage::Templates::Microarray::TwoChannel );
use strict;

sub getAllProfileSetNames {}

sub getProfileSetsArray {
  my @profileSetsArray = (['DHH1 induced vs. uninduced procyclics - wild type', 'standard error - DHH1 induced vs. uninduced procyclics - wild type', ],
                          ['DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', 'standard error - DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', ],
      );

  return \@profileSetsArray;
}

sub getPercentileSetsArray {
  my @percentileSetsArray = (['red percentile - DHH1 induced vs. uninduced procyclics - wild type', '', ['TEMP']],
                             ['red percentile - DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', '',, ['TEMP']],
                             ['green percentile - DHH1 induced vs. uninduced procyclics - wild type', '',, ['TEMP']],
                             ['green percentile - DHH1 induced vs. uninduced procyclics - DEAD:DQAD mutant', '',, ['TEMP']],
      );

  return \@percentileSetsArray;
}

sub getProfileRAdjust { return 'profile.df = t(as.matrix(colSums(profile.df, na.rm=T))); stderr.df = t(as.matrix(colSums(stderr.df, na.rm=T)))'}
sub getPercentileRAdjust { return 'profile.df = rbind(profile.df[1:2,1], profile.df[3:4,1]);stderr.df = 0;' }

1;

# TEMPLATE_ANCHOR microarraySimpleTwoChannelGraph
