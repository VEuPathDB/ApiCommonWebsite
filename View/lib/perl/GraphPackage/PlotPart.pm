package ApiCommonWebsite::View::GraphPackage::PlotPart;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage );
use ApiCommonWebsite::View::GraphPackage;
use ApiCommonWebsite::Model::CannedQuery::ElementNames;
use ApiCommonWebsite::Model::CannedQuery::Profile;
use ApiCommonWebsite::Model::CannedQuery::ProfileFixedValue;
use ApiCommonWebsite::View::GraphPackage::Util;

use Data::Dumper;

#----------------------------------------------------------------------------------------------

sub getPartName                  { $_[0]->{'_part_name'                     }}
sub setPartName                  { $_[0]->{'_part_name'                     } = $_[1]}

sub getProfileSetNames           { $_[0]->{'_profile_set_names'             }}
sub setProfileSetNames           { $_[0]->{'_profile_set_names'             } = $_[1]}

sub getStDevProfileSetNames      { $_[0]->{'_std_err_profile_set_names'      } || []}
sub getStErrProfileSetNames      { $_[0]->{'_std_err_profile_set_names'      } || []}
sub setStDevProfileSetNames      { $_[0]->{'_std_err_profile_set_names'      } = $_[1] }
sub setStErrProfileSetNames      { $_[0]->{'_std_err_profile_set_names'      } = $_[1] }

sub getYaxisLabel                { $_[0]->{'_y_axis_label'                  }}
sub setYaxisLabel                { $_[0]->{'_y_axis_label'                  } = $_[1]}

sub getColors                    { $_[0]->{'_colors'                        }}
sub setColors                    { $_[0]->{'_colors'                        } = $_[1]}

sub getIsLogged                  { $_[0]->{'_is_logged'                     }}
sub setIsLogged                  { $_[0]->{'_is_logged'                     } = $_[1]}

sub getPlotTitle                 { $_[0]->{'_plot_title'                    }}
sub setPlotTitle                 { $_[0]->{'_plot_title'                    } = $_[1]}

sub getMakeYAxisFoldInduction    { $_[0]->{'_make_y_axis_fold_induction'    }}
sub setMakeYAxisFoldInduction    { $_[0]->{'_make_y_axis_fold_induction'    } = $_[1]}

sub getAdjustProfile             { $_[0]->{'_r_adjust_profile'              }}
sub setAdjustProfile             { $_[0]->{'_r_adjust_profile'              } = $_[1]}

sub getDefaultYMax               { $_[0]->{'_default_y_max'                 }}
sub setDefaultYMax               { $_[0]->{'_default_y_max'                 } = $_[1]}

sub getDefaultYMin               { $_[0]->{'_default_y_min'                 }}
sub setDefaultYMin               { $_[0]->{'_default_y_min'                 } = $_[1]}

sub getSampleLabels               { $_[0]->{'_sample_labels'                  }}
sub setSampleLabels               { $_[0]->{'_sample_labels'                  } = $_[1]}


sub getProfileSetDisplayNames    {
  my ($self) = @_;
  if ($self->{'_profile_set_display_names'}) {
    return $self->{'_profile_set_display_names'};
  }
  return $self->getProfileSetNames;
}
sub setProfileSetDisplayNames    { $_[0]->{'_profile_set_display_names'         } = $_[1]}

#----------------------------------------------------------------------------------------------

sub getScreenSize                { $_[0]->{'_screen_size'                 }}
sub setScreenSize                { $_[0]->{'_screen_size'                 } = $_[1]}

sub getGraphDefaultValue         { $_[0]->{'_graph_default_value'         }}
sub setGraphDefaultValue         { $_[0]->{'_graph_default_value'         } = $_[1]}

sub getElementNameMarginSize          { $_[0]->{'_element_name_margin_size'          }}
sub setElementNameMarginSize          { $_[0]->{'_element_name_margin_size'          } = $_[1]}

sub getProfileFiles              { $_[0]->{'_profile_files'               }}
sub setProfileFiles              { $_[0]->{'_profile_files'               } = $_[1]}
sub addProfileFile {
  my ($self, $file) = @_;

  push @{$self->getProfileFiles()}, $file;
}

sub getElementNameFiles         { $_[0]->{'_element_name_files'           }}
sub setElementNameFiles         { $_[0]->{'_element_name_files'           } = $_[1]}
sub addElementNameFile {
  my ($self, $file) = @_;
  push @{$self->getElementNameFiles()}, $file;
}


#----------------------------------------------------------------------------------------------

sub hasGraphDefault {
  my ($self) = @_;

  if(defined($self->getGraphDefaultValue())) {
    return 1;
  }
  return 0;
}

#----------------------------------------------------------------------------------------------

sub new {
   my $class = shift;
   my $args = ref $_[0] ? shift :{};

   my $self = bless $args, $class;

   #Setting Defaults
   $self->setScreenSize(250);
   $self->setElementNameMarginSize(3);
   $self->setYaxisLabel('Please Fill in Y-Axis Label');
#   $self->setIsLogged(0);
   $self->setDefaultYMax(10);
   $self->setDefaultYMin(0);
   $self->setColors(["#000099"]);
   return $self;
}

sub init {
  my ($self) = @_;

  $self->SUPER::init(@_);

  # Default 
  $self->setProfileFiles([]);
  $self->setElementNameFiles([]);
  $self->setSampleLabels([]);
  $self;
}

sub writeProfileFiles {
  my ($self, $profileSetName, $suffix, $elementOrder) = @_;
  my $_qh   = $self->getQueryHandle();
  my $_dict = {};

  my $defaultProfile;

  if($self->hasGraphDefault()) {
    my $defaultValue = $self->getGraphDefaultValue();

    $defaultProfile = ApiCommonWebsite::Model::CannedQuery::ProfileFixedValue->new
      ( Name         => "_data_$suffix",
        Id           => $self->getId(),
        ProfileSet   => $profileSetName,
        DefaultValue => $defaultValue,
      );
  }


  my $profile = ApiCommonWebsite::Model::CannedQuery::Profile->new
    ( Name         => "_data_$suffix",
      Id           => $self->getId(),
      ProfileSet   => $profileSetName,
    );

  my $elementNames = ApiCommonWebsite::Model::CannedQuery::ElementNames->new
      ( Name         => "_names_$suffix",
        Id           => $self->getId(),
        ProfileSet   => $profileSetName,
      );

  my @profileErrors = [];
  my @errors = [];

  $profile->prepareDictionary($_dict);

  $profile->setElementOrder($elementOrder) if($elementOrder);
  $elementNames->setElementOrder($elementOrder) if($elementOrder);

  my $profile_fn = eval { $profile->makeTabFile($_qh, $_dict) }; $@ && push(@profileErrors, $@);
  my $elementNames_fn = eval { $elementNames->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
  $self->addProfileFile($profile_fn) if($profile_fn);
  $self->addElementNameFile($elementNames_fn) if($elementNames_fn);

#  if(@profileErrors) {
#    $profile_fn = eval { $defaultProfile->makeTabFile($_qh, $_dict) }; $@ && push(@errors, $@);
#    $self->addTempFile($profile_fn) if($profile_fn);
#  }
  
  my @rv = ($profile_fn, $elementNames_fn);



  return \@rv;
}

1;
