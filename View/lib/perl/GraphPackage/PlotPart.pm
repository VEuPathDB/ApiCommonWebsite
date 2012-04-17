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

sub getProfileSets               { $_[0]->{'_profile_sets'           }}
sub setProfileSets               { $_[0]->{'_profile_sets'           } = $_[1]}

sub getPartName                  { $_[0]->{'_part_name'                     }}
sub setPartName                  { $_[0]->{'_part_name'                     } = $_[1]}

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

sub getRPostscript               { $_[0]->{'_r_postscript'              }}
sub setRPostscript               { $_[0]->{'_r_postscript'              } = $_[1]}

#----------------------------------------------------------------------------------------------

sub getScreenSize                { $_[0]->{'_screen_size'                 }}
sub setScreenSize                { $_[0]->{'_screen_size'                 } = $_[1]}

sub getElementNameMarginSize          { $_[0]->{'_element_name_margin_size'          }}
sub setElementNameMarginSize          { $_[0]->{'_element_name_margin_size'          } = $_[1]}

#----------------------------------------------------------------------------------------------

sub new {
  my $class = shift;

   my $self = $class->SUPER::new(@_);

  my $id = $self->getId();

   #Setting Defaults
   $self->setScreenSize(250);
   $self->setElementNameMarginSize(3);
   $self->setYaxisLabel('Please Fill in Y-Axis Label');
   $self->setDefaultYMax(10);
   $self->setDefaultYMin(0);
   $self->setColors(["#000099"]);

  $self->setPlotTitle($id);

  $self->setSampleLabels([]);

   return $self;
}

#----------------------------------------------------------------------------------------------

sub makeFilesForR {
  my ($self) = @_;

  my $part = $self->getPartName();
  my $profileSampleLabels = $self->getSampleLabels();

  my $profileSets = $self->getProfileSets();
  my $id = $self->getId();
  my $qh = $self->getQueryHandle();

  for(my $i = 0; $i < scalar @$profileSets; $i++) {
    my $profileSet = $profileSets->[$i];
    my $suffix = $part . $i;

    $profileSet->writeFiles($id, $qh, $suffix);
  }

  my @profileFiles = map { $_->getProfileFile() } @$profileSets;
  my @elementNamesFiles = map { $_->getElementNamesFile() } @$profileSets;

  my @stderrProfileSets = map { $_->getRelatedProfileSet() } @$profileSets;
  my @stderrFiles = map { $_->getProfileFile() if($_) } @stderrProfileSets;

  my $profileFilesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@profileFiles, 'profile.files');
  my $elementNamesString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@elementNamesFiles, 'element.names.files');
  my $stderrString = ApiCommonWebsite::View::GraphPackage::Util::rStringVectorFromArray(\@stderrFiles, 'stderr.files');

  print STDERR Dumper \@profileFiles;
  print STDERR Dumper \@elementNamesFiles;
  print STDERR Dumper \@stderrFiles;

  return($profileFilesString, $elementNamesString, $stderrString);
}




1;
