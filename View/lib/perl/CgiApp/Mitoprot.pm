# Test module for CgiApp                                                                                                            
# returns Prediction of mitochondrial targeting sequences and reports if                                                                      
# running in  cgi or Apache::Registry environment                                                                                   

package ApiCommonWebsite::View::CgiApp::Mitoprot;
@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use URI::Escape;
use LWP::UserAgent;
use HTTP::Request;
#use HTTP::Request::Common qw(POST);
use EbrcWebsiteCommon::View::CgiApp;
use LWP::Protocol::https;
use Data::Dumper;


sub run {
    
    my ($self,$cgi) = @_;
    
    my $project_id = $cgi->param('project_id');
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
    
    
#skip ssl verify &  be aware that you can turn off verification in your script on the creation of a new LWP::UserAgent instance by:
    
    my $ua = LWP::UserAgent->new(
	ssl_opts => { verify_hostname => 0 },
	);
    
    
    
# In this case, we don't need the following comment parts 
=head
    
    my $args = "CMD=Put&seq=" . $seq;
    my $req = new HTTP::Request GET => 'https://ihg.gsf.de/cgi-bin/paolo/mitofilter';
    
    $req->content_type('application/x-www-form-urlencoded');
    $req->content($args);

    # get the response                                                                                                              
    my $response = $ua->request($req);
=cut



   #retrieve and display results  
    print "Location: https://ihg.gsf.de/cgi-bin/paolo/mitofilter?seq=$seq&seqname="."\n\n";

}

1;









	
 
