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
    my $id_type = $cgi->param('id_type');

    my $qh = $self->getQueryHandle($cgi);

    my $sql;

    if($id_type eq  'protein'){

        $sql = "select SEQUENCE from APIDBTUNING.PROTEINSEQUENCE where SOURCE_ID = '" . $source_ID . "' and PROJECT_ID= '" . $project_id . "'";
    }else{
       
	die "cannot find the protein sequence from database based on entered source_ID and PROJECT_ID" ;
    }

    my $sh = $qh->prepare($sql);

    $sh->execute();

    my $seq = $sh->fetchrow_array();

    $sh->finish();


    my $ua = LWP::UserAgent->new;

    my $args = "CMD=Put&organism_type=$organism_type&input_type=$input_type&fasta_input=" . $seq;

    my $req = new HTTP::Request POST => 'https://wolfpsort.hgc.jp/?submitted=1';
    

    $req->content_type('application/x-www-form-urlencoded');
    $req->content($args);


    # get the response                                                                                                              
    my $response = $ua->request($req);

    #parse out the request id                                                                                                    

    my $id;

    if ($response->content =~ /id=(\w*)"/) {
	$id = $1;
    } else {
	print "didn't match\n";
    }

   # retrieve and display results  
    print "Location: https://wolfpsort.hgc.jp/?submitted=1&id=$id"."\n\n";

}

1;





	
