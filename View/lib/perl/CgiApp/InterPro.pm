# Test module for CgiApp                                                                                                            
# returns Protein Prediction and reports if                                                                      
# running in  cgi or Apache::Registry environment                                                                                   

package ApiCommonWebsite::View::CgiApp::InterPro;
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
    #my $queryString = $cgi->param('queryString');
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


    # get the response                                                                                                             
 
    my $response =
        $ua->post('https://www.ebi.ac.uk/interpro/',
		  { "queryString" => "$seq",
                         "leaveIt" => ""
		  }
        );

    #print STDERR Dumper $response;

    print "Location: ". $response->headers->{location}."\n\n";
    
    #exit;


    

}
1;






	
 
