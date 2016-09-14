package ApiCommonWebsite::View::GraphPackage::Templates::SpliceSites;


use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::Templates::Expression );
use ApiCommonWebsite::View::GraphPackage::Templates::Expression;
use Data::Dumper;

# @Override
sub getGroupRegex {
  return qr/\[counts\]/;
}

# @Override
sub getRemainderRegex {
  return qr/(\S+) \[counts\]/;
}




1;


#--------------------------------------------------------------------------------


# TEMPLATE_ANCHOR spliceSitesGraph

