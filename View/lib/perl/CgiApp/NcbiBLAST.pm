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

use Data::Dumper;


sub run {
    
    my ($self,$cgi) = @_;
    
    my $project_id = $cgi->param('project_id');
    my $program = $cgi->param('program');
    my $database = $cgi->param('database');
    my $source_ID = $cgi->param('source_ID');
    my $ID_Type = $cgi->param('ID_Type');
   


    my $qh = $self->getQueryHandle($cgi);

    #print $cgi->header('text/html');

    my $sql;
    
    if($ID_Type== 'protein'){
	
	$sql = "select SEQUENCE from APIDBTUNING.PROTEINSEQUENCE where SOURCE_ID = '" . $source_ID . "' and PROJECT_ID= '" . $project_id . "'";
    }else{
	#$self->error();
	die "cannot find the protein sequence from database based on entered source_ID and PROJECT_ID" ;
    }
   
    my $sh = $qh->prepare($sql);  
    
    $sh->execute();
    
    my $seq = $sh->fetchrow_array();
    
    $sh->finish();
 
    
    my $ua = LWP::UserAgent->new;


=head
    my $argc = $#ARGV + 1;

    if ($argc < 5)
    {
	print "usage: ncbiBLAST.pl project_id program database source_id ID_Type...\n\n";
	print "where program = megablast, blastn, blastp, rpsblast, blastx, tblastn, tblastx\n\n";
	print "Here's an example: ncbiBLAST.pl PlasmoDB blastp nr AK88_00007-t30_1-p1 protein\n";
	
	exit ;
    } 
=cut

    if ($program eq "megablast")
    {
	$program = "blastn&MEGABLAST=on";
    }
    
    if ($program eq "rpsblast")
    {
	$program = "blastp&SERVICE=rpsblast";
    }
    
    
    
    my $encoded_query=$seq;
    
    my $args = "CMD=Put&PROGRAM=$program&DATABASE=$database&QUERY=" . $encoded_query;
    
    my $req = new HTTP::Request POST => 'https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi';
    
    
    $req->content_type('application/x-www-form-urlencoded');
    $req->content($args);
       

    # get the response
    my $response = $ua->request($req);
    
    # parse out the request id
    $response->content =~ /^    RID = (.*$)/m;
    my $rid=$1;
    
    # parse out the estimated time to completion
    $response->content =~ /^    RTOE = (.*$)/m;
    my $rtoe=$1;

    # wait for search to complete
    sleep $rtoe;

   
    # poll for results
    while (1)
    {
	sleep 5;
	
	$req = new HTTP::Request GET => "https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi?CMD=Get&FORMAT_OBJECT=SearchInfo&RID=$rid";
	$response = $ua->request($req);
	
	if ($response->content =~ /\s+Status=WAITING/m)
	{
	    #print STDERR "Searching...\n";
	    next;
	}
	
	if ($response->content =~ /\s+Status=FAILED/m)
	{
	    #print STDERR "Search $rid failed; please report to blast-help\@ncbi.nlm.nih.gov.\n";
	    exit 4;
	}
	
	if ($response->content =~ /\s+Status=UNKNOWN/m)
	{
	    #print STDERR "Search $rid expired.\n";
	    exit 3;
	}
	
	if ($response->content =~ /\s+Status=READY/m) 
	{
	    if ($response->content =~ /\s+ThereAreHits=yes/m)
	    {
		#print STDERR "Search complete, retrieving results...\n";
		last;
	    }
	    else
	    {
		#print STDERR "No hits found.\n";
		exit 2;
	    }
	}
	
	# if we get here, something unexpected happened.
	exit 5;
    } # end poll loop
    
    # retrieve and display results

    my $req = new HTTP::Request GET => "https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi?NCBI_GI=T&FORMAT_OBJECT=Alignment&ALIGNMENTS=5&CMD=Get&FORMAT_TYPE=HTML&RID=$rid";
    
     print "Location: https://blast.ncbi.nlm.nih.gov/blast/Blast.cgi?NCBI_GI=T&FORMAT_OBJECT=Alignment&ALIGNMENTS=5&CMD=Get&FORMAT_TYPE=HTML&RID=$rid"."\n\n";
       
}



sub error{
    #my ($msg) = @_;

    #print "ERROR: Please make sure the source_ID should be protein\n\n";
    exit(1);


}

1;



