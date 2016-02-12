package ApiCommonWebsite::View::GraphPackage::Templates::ExpressionTwoChannel;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;

sub finalProfileAdjustments {
  my ($self, $profile) = @_;
  my $plotPart = $profile->getPartName();
  if ($plotPart =~/percentile/) {
    $profile->setHasExtraLegend(1); 
    $profile->setLegendLabels(['channel 1', 'channel 2']);
    $profile->setColors(['LightSlateGray', 'DarkSlateGray']);
  }
}
1;

#--------------------------------------------------------------------------------

# TEMPLATE_ANCHOR microarraySimpleTwoChannelGraph
