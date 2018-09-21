# Test module for CgiApp                                                                                                            
# The ChloroP server predicts the presence of chloroplast transit peptides (cTP) in protein sequences and the location of potential# cTP cleavage sites.
                                                                      
# running in  cgi or Apache::Registry environment                                                                                   

package ApiCommonWebsite::View::CgiApp::ChloroP;
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
    my $source_ID = $cgi->param('source_ID');
    my $id_type = $cgi->param('id_type');
    #my $configfile = $cgi->param('configfile');
    #my $full = $cgi->param('full');


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
   
    # get the response 
    my $response =
	$ua->post('http://www.cbs.dtu.dk/cgi-bin/webface2.fcgi',
		       { "SEQPASTE" => "$seq",
			 full => 'full',
			 "configfile" => 
			     "/usr/opt/www/pub/CBS/services/ChloroP-1.1/chlorop.cf"
		       }
	);

    
    #print Dumper($response);
    #print STDERR $response->headers->{location} . "\n\n";

    
    #retrieve and display results                                                                                                   
    print "Location: http://www.cbs.dtu.dk". $response->headers->{location}. "\n\n";


}



1;
	
