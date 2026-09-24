package ApiCommonWebsite::View::CgiApp::UniprotDBXRefs;
@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use EbrcWebsiteCommon::View::CgiApp;

use constant PORTAL_PROJECT => 'UniDB';

sub init {
  my $self = shift;

  $self->setProjectId($self->cla()->param('project_id') || PORTAL_PROJECT);

  return $self;
}

sub run {
  my ($self, $cgi) = @_;

  print STDOUT $cgi->header('text/plain');

  my $projectId = $self->getProjectId();
  my $isPortal = $projectId eq PORTAL_PROJECT;

  my $dbh = $self->getQueryHandle($cgi);
  my $sql = "select ga.project_id,
       ga.source_id,
       gi.id as uniprot
from apidbtuning.GeneId gi, apidbtuning.GeneAttributes ga
where ga.source_id = gi.gene
and gi.database_name ilike '%uniprot%'";
  $sql .= "\nand ga.project_id = ?" unless $isPortal;

  my $sth = $dbh->prepare($sql);
  $sth->execute($isPortal ? () : ($projectId));

  print "uniprotID\teupathid\n";

  while (my ($project, $source_id, $uniprot) = $sth->fetchrow_array) {
    print "$uniprot\t$project:$source_id\n";
  }

  $sth->finish();

}

1;

