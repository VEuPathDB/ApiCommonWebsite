# Test module for CgiApp                                                                                                            
# returns Protein Subcellular Localization Prediction and reports if                                                                      
# running in  cgi or Apache::Registry environment                                                                                   

package ApiCommonWebsite::View::CgiApp::WolfPSORT;
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
    my $organism_type = $cgi->param('organism_type');
    my $input_type=$cgi->param('input_type');
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


    my $encoded_query=$seq;

    my $args = "CMD=Put&organism_type=$organism_type&input_type=$input_type&fasta_input=" . $encoded_query;

    my $req = new HTTP::Request POST => 'https://wolfpsort.hgc.jp/?submitted=1';
    

    $req->content_type('application/x-www-form-urlencoded');
    $req->content($args);


    # get the response                                                                                                              
    my $response = $ua->request($req);
    #print Dumper($response);

    #parse out the request id                                                                                                    
    my $id;

    if ($response->content =~ /id=(\w*)"/) {
	$id = $1;
    } else {
	print "didn't match\n";
    }
  

    #poll for results                                                                                                              
    $req = new HTTP::Request GET => "https://wolfpsort.hgc.jp/?submitted=1&id=$id";

   # retrieve and display results  
    print "Location: https://wolfpsort.hgc.jp/?submitted=1&id=$id"."\n\n";



}


sub error{
    #my ($msg) = @_;                                                                                                                

    #print "ERROR: Please make sure the source_ID should be protein\n\n";                                                           
    exit(1);


}

1;





	
