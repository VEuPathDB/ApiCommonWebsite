# Test module for CgiApp
# returns protein alignment from blastp vs NRDB and reports if 
# running in  cgi or Apache::Registry environment

package ApiCommonWebsite::View::CgiApp::NcbiBLAST;
@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use URI::Escape;
use LWP::UserAgent;
use HTTP::Request::Common qw(POST);
use EbrcWebsiteCommon::View::CgiApp;

sub run {
    my ($self,$cgi) = @_;
    
    my $project_id = $cgi->param('project_id');
    my $program = $cgi->param('program');
    my $database = $cgi->param('database');
    my $source_ID = $cgi->param('source_ID');
    my $id_type = $cgi->param('id_type');

    my $qh = $self->getQueryHandle($cgi);

    my $sql;
    
    if($id_type eq 'protein'){
	$sql = "select SEQUENCE from APIDBTUNING.PROTEINSEQUENCE where SOURCE_ID = '" . $source_ID . "' and PROJECT_ID= '" . $project_id . "'";
    }else{
	die "cannot find the protein sequence from database based on entered source_ID and PROJECT_ID" ;
    }
   
    my $sh = $qh->prepare($sql);  
    $sh->execute();

    my $seq = $sh->fetchrow_array();
    $sh->finish();
    
    my $ua = LWP::UserAgent->new;

    my $args = "CMD=Put&PROGRAM=$program&DATABASE=$database&QUERY=" . $seq;
    
    my $req = new HTTP::Request POST => 'https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi';
    
    $req->content_type('application/x-www-form-urlencoded');
    $req->content($args);

    # get the response
    my $response = $ua->request($req);
    
    # parse out the request id
    $response->content =~ /^    RID = (.*$)/m;
    my $rid=$1;
    
    print "Location: https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi?NCBI_GI=T&FORMAT_OBJECT=Alignment&ALIGNMENTS=5&CMD=Get&FORMAT_TYPE=HTML&RID=$rid"."\n\n";
}



1;



