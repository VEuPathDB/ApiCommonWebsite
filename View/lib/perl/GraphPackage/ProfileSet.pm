package ApiCommonWebsite::View::GraphPackage::ProfileSet;

use strict;

# Main Profile Set Name
sub getName                      { $_[0]->{'_name'             }}
sub setName                      { $_[0]->{'_name'             } = $_[1]}

sub getElementNames              { $_[0]->{'_element_names'                  }}
sub setElementNames              { $_[0]->{'_element_names'                  } = $_[1]}

sub getRelatedProfileSet         { $_[0]->{'_related_profile_set'             }}
sub setRelatedProfileSet         { $_[0]->{'_related_profile_set'             } = $_[1]}

sub getDisplayName    {
  my ($self) = @_;
  if ($self->{'_display_name'}) {
    return $self->{'_display_name'};
  }
  return $self->getName();
}
sub setDisplayName    { $_[0]->{'_display_name'         } = $_[1]}

sub getProfileFile              { $_[0]->{'_profile_file'               }}
sub setProfileFile              { $_[0]->{'_profile_file'               } = $_[1]}

sub getElementNamesFile         { $_[0]->{'_element_names_file'           }}
sub setElementNamesFile         { $_[0]->{'_element_names_file'           } = $_[1]}

#--------------------------------------------------------------------------------
# sub getGraphDefaultValue         { $_[0]->{'_graph_default_value'         }}
# sub setGraphDefaultValue         { $_[0]->{'_graph_default_value'         } = $_[1]}

# sub hasGraphDefault {
#   my ($self) = @_;

#   if(defined($self->getGraphDefaultValue())) {
#     return 1;
#   }
#   return 0;
# }



sub new {
  my ($class, $name, $elementNames) = @_;

  unless($name) {
    die "ProfileSet Name missing: $!";
  }

  my $self = bless {}, $class;

  $self->setName($name);

  unless(ref($elementNames) eq 'ARRAY') {
    $elementNames = [];
  }
  $self->setElementNames($elementNames);

  return $self;
}

sub writeFiles {
  my ($self, $id, $qh, $suffix) = @_;

  $self->writeProfileFile($id, $qh, $suffix);
  $self->writeElementNamesFile($id, $qh, $suffix);

  # don't need to write the element names file for the related set
  if(my $relatedProfileSet = $self->getRelatedProfileSet()) {
    $suffix = "related" . $suffix;
    $relatedProfileSet->writeProfileFile($id, $qh, $suffix);
  }
}

sub writeProfileFile {
  my ($self, $id, $qh, $suffix) = @_;

  my $_dict = {};

  my $profileSetName = $self->getName();
  my $elementNames = $self->getElementNames();

  my $profile = ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => "_data_$suffix",
      Id           => $id,
      ProfileSet   => $profileSetName,
    );

  $profile->prepareDictionary($_dict);
  $profile->setElementOrder($elementNames) if(scalar @$elementNames > 0);

  my $profile_fn = $profile->makeTabFile($qh, $_dict);

  $self->setProfileFile($profile_fn);

}

sub writeElementNamesFile {
  my ($self, $id, $qh, $suffix) = @_;

  my $_dict = {};

  my $elementNames = $self->getElementNames();
  my $profileSetName = $self->getName();

  my $elementNamesProfile = ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => "_names_$suffix",
        Id           => $id,
        ProfileSet   => $profileSetName,
      );

  $elementNamesProfile->setElementOrder($elementNames) if(scalar @$elementNames > 0);

  my $elementNames_fn = $elementNamesProfile->makeTabFile($qh, $_dict);

  $self->setElementNamesFile($elementNames_fn);
}






1;
