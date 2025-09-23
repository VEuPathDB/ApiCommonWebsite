package ApiCommonWebsite::View::CgiApp::UniprotDBXRefs;
@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use EbrcWebsiteCommon::View::CgiApp;

sub run {
  my ($self, $cgi) = @_;

  print STDOUT $cgi->header('text/plain');

  my $dbh = $self->getQueryHandle($cgi);
  my $sql = "select ga.project_id, 
       ga.source_id, 
       gi.id as uniprot
from webready.GeneId gi, webready.GeneAttributes ga
where ga.source_id = gi.gene
and (gi.database_name like '%uniprot_dbxref_RSRC'
or gi.database_name like '%dbxref_gene2Uniprot_RSRC'
or gi.database_name like '%dbxref_uniprot_linkout_RSRC'
or gi.database_name like '%dbxref_uniprotkb_from_annotation_RSRC'
or gi.database_name like '%dbxref_simple_gene2Uniprot_RSRC'
or gi.database_name = 'Links to Uniprot Genes'
or gi.database_name like 'Uniprot%'
)";
  
  my $sth = $dbh->prepare($sql);
  $sth->execute();

  print "uniprotID\teupathid\n";
      
  while (my ($project, $source_id, $uniprot) = $sth->fetchrow_array) {
    print "$uniprot\t$project:$source_id\n";
  }
  
  $sth->finish();

}

1;

