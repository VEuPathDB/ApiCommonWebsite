
package ApiCommonWebsite::View::CgiApp::STRING;
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
    

    #my $required_score = $cgi->param('required_score');
    #my $sessionId = $cgi->param('sessionId');
    #my $have_user_input = $cgi->param('have_user_input');
    my $organism = $cgi->param('organism');

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
    
    #my $ua = LWP::UserAgent->new;
    
    my $ua = LWP::UserAgent->new(agent => 'Mozilla');
    

    print "Location: http://string-db.org/newstring_cgi/show_network_section.pl?sequence=$seq&species_text_single_sequence=$organism&amp;external_payload_URL=http://your-server.org/ext.json" . "\n\n";


=head
    my $response =
	$ua->post('http://string-db.org/newstring_cgi/show_network_section.pl',
		  { "sequence" => $seq,
		    "species_text_single_sequence" => $organism,
		    "required_score" => "400",
		    "sessionId" => "gAB1erjz7Ssq",
		    "have_user_input" => "2"
		  }
	);
    print STDERR Dumper $response;
    #print $cgi->header("text/html");

=cut

}



1;




