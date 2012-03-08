package ApiCommonWebsite::View::GraphPackage::Util;

use strict;


sub rStringVectorFromArray {
  my ($stringArray, $name) = @_;

  return "$name = c(" . join(',', map {"\"$_\""} @$stringArray) . ");";
}

sub rNumericVectorFromArray {
  my ($array, $name) = @_;

  return "$name = c(" . join(',', map {"$_"} @$array) . ");";
}

sub isSeen {
  my ($x, $ar) = @_;

  foreach(@$ar) {
    return 1 if($_->{name} eq $x);
  }
  return 0;
}

1;
