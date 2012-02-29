package ApiCommonWebsite::View::GraphPackage::MixedPlotSet;

use strict;
use vars qw( @ISA );

@ISA = qw( ApiCommonWebsite::View::GraphPackage::AbstractPlotSet );
use ApiCommonWebsite::View::GraphPackage::AbstractPlotSet;

#--------------------------------------------------------------------------------
# NOTE:  Each Graph Object makes an Array of Hashes.  The Hash Keys Must Be 
#        unique accross all combined graphs for this code to work.
#--------------------------------------------------------------------------------

sub getGraphObjects { $_[0]->{_graph_objects} }
sub setGraphObjects { 
  my $self = shift;

  unless(scalar @_ >= 1) {
    die "Graph Ojbects Array Not Defined Correctly" ;
  }

  my %profileSetParts;

  my $graphs = [];
  foreach my $graph (@_) {

    # Check that all graph parts are unique
    my $psh = $graph->getProfileSetsHash();
    foreach my $part (keys %$psh) {
      if($profileSetParts{$part}) {
        die "Seen Key [$part] More than once";
      }
      $profileSetParts{$part}++;
    }
    push @{$graphs}, $graph;
  }

  # Each Graph Ojbect Defines its own ProfileSetsHash
  # This objects ProfileSetsHash only needs to state the union of the parts as hash keys
  $self->setProfileSetsHash(\%profileSetParts);

  $self->{_graph_objects} = $graphs; 
}


#--------------------------------------------------------------------------------

sub init {
  my $self = shift;
  my $args = ref $_[0] ? shift : {@_};

  $self->SUPER::init($args);

  # Defaults
  $self->setScreenSize(225);
  $self->setBottomMarginSize(5);

  return $self;
}

#--------------------------------------------------------------------------------


sub makeRPlotStrings {
  my ($self) = @_;

  my $graphObjects = $self->getGraphObjects();

  my $ms = $self->getMultiScreen();
  my $id = $self->getId();
  my $name = $self->getName();
  my $qh = $self->getQueryHandle();
  my $format = $self->getFormat();
  my $output = $self->getOutputFile();
  my $thumb = $self->getThumbnail();
  my $vp = $self->getVisibleParts();
  my $secId = $self->getSecondaryId();
  my $dp = $self->getDataPlotterArg();

  my $tempFiles = $self->getTempFiles();

  my @rv;

  foreach my $graph (@$graphObjects) {
    $graph->setMultiScreen($ms);
    $graph->setId($id);
    $graph->setName($name);
    $graph->setQueryHandle($qh);
    $graph->setFormat($format);
    $graph->setOutputFile($output);
    $graph->setThumbnail($thumb);
    $graph->setVisibleParts($vp);
    $graph->setSecondaryId($secId);
    $graph->setDataPlotterArg($dp);
    $graph->setTempFiles($tempFiles);


    push @rv, @{$graph->makeRPlotStrings()};
  }
  return \@rv;
}

#--------------------------------------------------------------------------------


1;
