package ApiCommonWebsite::View::GraphPackage::Util;

use strict;

use ApiCommonWebsite::View::GraphPackage::ProfileSet;

sub makeProfileSets {
  my ($arr) = @_;

  my @rv;

  foreach my $row (@$arr) {
    my $mainProfileSet = $row->[0];
    my $relatedProfileSet = $row->[1];
    my $elementNames = $row->[2];
    my $alternateSourceId = $row->[3];
    my $scale = $row->[4];

    my $profileSet = ApiCommonWebsite::View::GraphPackage::ProfileSet->new($mainProfileSet, $elementNames, $alternateSourceId, $scale);

    if($relatedProfileSet) {
      my $relatedSet = ApiCommonWebsite::View::GraphPackage::ProfileSet->new($relatedProfileSet, $elementNames, $alternateSourceId, $scale);
      $profileSet->setRelatedProfileSet($relatedSet);
    }
    push @rv, $profileSet;
  }
  return \@rv;
}



sub rStringVectorFromArray {
  my ($stringArray, $name) = @_;

  return "$name = c(" . join(',', map { defined $_ ? "\"$_\"" : "\"\""} @$stringArray) . ");";
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
