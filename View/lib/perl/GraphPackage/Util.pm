package ApiCommonWebsite::View::GraphPackage::Util;

use strict;

use Math::Round;

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
    my $metaDataCategory = $row->[5];
    my $mainProfileSetDisplayName = $row->[6];

    my $profileSet = ApiCommonWebsite::View::GraphPackage::ProfileSet->new($mainProfileSet, $elementNames, $alternateSourceId, $scale, $metaDataCategory, $mainProfileSetDisplayName);

    if($relatedProfileSet) {
      my $relatedSet = ApiCommonWebsite::View::GraphPackage::ProfileSet->new($relatedProfileSet, $elementNames, $alternateSourceId, $scale, $metaDataCategory);
      $profileSet->setRelatedProfileSet($relatedSet);
    }
    push @rv, $profileSet;
  }
  return \@rv;
}



sub getProfileSetsSql {
  return "select ps.name,d.name
from apidb.profileset ps,
sres.externaldatabase d, 
sres.externaldatabaserelease r
where ps.external_database_release_id = r.external_database_release_id
and r.external_database_id = d.external_database_id
and d.name = ?"

}

sub getFilteredProfileSetsSql {
   return "select ps.name
from apidb.profileset ps,
apidb.profile p,
sres.externaldatabase d, 
sres.externaldatabaserelease r,
(select dsnt.name 
 from APIDBTUNING.datasetpresenter dp, 
      APIDBTUNING.datasetnametaxon dsnt
 where dp.dataset_presenter_id = dsnt.dataset_presenter_id
   and dp.name = ?) dp
where ps.external_database_release_id = r.external_database_release_id
and r.external_database_id = d.external_database_id
and dp.name = d.name
and p.profile_set_id = ps.profile_set_id
and p.source_id = ?"
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
