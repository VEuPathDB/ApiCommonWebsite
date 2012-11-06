package ApiCommonWebsite::Model::ModelApicommonXml;

use strict;
use XML::Twig;


sub getSiteVersions {$_[0]->{_site_versions}}
sub getBuildNumbers {$_[0]->{_build_numbers}}

sub getSiteVersionByProjectId {
  my ($self, $projectId) = @_;


  my $siteVersions = $self->getSiteVersions();

  if(my $siteVersion = $siteVersions->{$projectId}) {
    return $siteVersion;
  }
  die "No Release Version found for project $projectId";
}

sub getBuildNumberByProjectId {
  my ($self, $projectId) = @_;

  my $buildNumbers = $self->getBuildNumbers();

  if(my $buildNumber = $buildNumbers->{$projectId}) {
    return $buildNumber;
  }
  die "No Build Number found for project $projectId";
}

sub new {
  my ($class) = @_;
  my $self = {};
  bless $self;
        
  my $propfile = "$ENV{GUS_HOME}/lib/wdk/apiCommonModel.xml";

  my %site_versions;
  my %build_numbers;

  my $twig = new XML::Twig(keep_spaces => 1,  
                           PrettyPrint => 'nice',
                           keep_atts_order => 1,
                           TwigHandlers => {
                             'constant[@name="releaseVersion"]'  => sub { 
                               $site_versions{$_[1]->att("includeProjects")} = $_[1]->text;
                             },  
                             'constant[@name="buildNumber"]'  => sub { 
                               $build_numbers{$_[1]->att("includeProjects")} = $_[1]->text;
                             },
                           }   
                           );  
  
  $twig->parsefile($propfile); 


  foreach(keys %site_versions) {
    my $value = $site_versions{$_};
    my @a = split(/,/, $_);
    foreach(@a) {
      $site_versions{$_} = $value;
    }
  }

  foreach(keys %build_numbers) {
    my $value = $build_numbers{$_};
    my @a = split(/,/, $_);
    foreach(@a) {
      $build_numbers{$_} = $value;
    }
  }


  $self->{_site_versions} = \%site_versions;
  $self->{_build_numbers} = \%build_numbers;
  
  return $self;
}


1;
