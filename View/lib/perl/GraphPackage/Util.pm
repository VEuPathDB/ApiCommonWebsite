package ApiCommonWebsite::View::GraphPackage::Util;

use strict;

use Math::Round;

use ApiCommonWebsite::View::GraphPackage::ProfileSet;

sub makeProfileSets {
  my ($arr) = @_;

  my @rv;

  foreach my $row (@$arr) {
    my $mainProfileSet = $row->[0];
    my $mainProfileType = $row->[1];
    my $relatedProfileSet = $row->[2];
    my $relatedProfileType = $row->[3];
    my $elementNames = $row->[4];
    my $alternateSourceId = $row->[5];
    my $scale = $row->[6];
    my $metaDataCategory = $row->[7];
    my $mainProfileSetDisplayName = $row->[8];

    my $profileSet = ApiCommonWebsite::View::GraphPackage::ProfileSet->new($mainProfileSet, $mainProfileType, $elementNames, $alternateSourceId, $scale, $metaDataCategory, $mainProfileSetDisplayName);

    if($relatedProfileSet) {
      my $relatedSet = ApiCommonWebsite::View::GraphPackage::ProfileSet->new($relatedProfileSet, $relatedProfileType, $elementNames, $alternateSourceId, $scale, $metaDataCategory);
      $profileSet->setRelatedProfileSet($relatedSet);
    }
    push @rv, $profileSet;
  }
  return \@rv;
}



sub getProfileSetsSql {
  return "SELECT DISTINCT pt.profile_set_name, pt.profile_type
FROM apidbtuning.ProfileType pt, apidbtuning.DatasetNameTaxon dnt
WHERE pt.dataset_name = dnt.name
AND dnt.dataset_presenter_id = ?"
}


sub rStringVectorFromArray {
  my ($stringArray, $name) = @_;

  return "$name = c(" . join(',', map { defined $_ ? "\"$_\"" : "\"\""} @$stringArray) . ");";
}

sub rNumericVectorFromArray {
  my ($array, $name) = @_;

  return "$name = c(" . join(',', map {"$_"} @$array) . ");";
}

sub rBooleanVectorFromArray {
  &rNumericVectorFromArray(@_);
}

sub isSeen {
  my ($x, $ar) = @_;

  foreach(@$ar) {
    return 1 if($_->{name} eq $x);
  }
  return 0;
}

sub getLighterColorFromHex {
  my ($color) = @_;

  unless($color =~ /^\#\w\w\w\w\w\w$/) {
    print STDERR "Must use hex values for input color\n";
    return $color
  }

  my @col = (hex(substr($color, 1, 2)),
             hex(substr($color, 3, 2)),
             hex(substr($color,5, 2))
             );

  my @lighter = (255 - (255 - $col[0]) / 4,
                 255 - (255 - $col[1]) / 4,
                 255 - (255 - $col[2]) / 4
                 );

  return "#" . sprintf("%02X%02X%02X", $lighter[0], $lighter[1], $lighter[2]);
}

1;
