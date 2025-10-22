# returns Fungal GPI Modification Site Prediction and reports if running in  cgi or Apache::Registry environment

package ApiCommonWebsite::View::CgiApp::FungalGPI;
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
    my $LSet = $cgi->param('LSet');
    my $source_ID = $cgi->param('source_ID');
    my $id_type = 'protein';

    my $qh = $self->getQueryHandle($cgi);
    my $sql;

    if($id_type eq  'protein'){
        $sql = "SELECT sequence FROM webready.ProteinSequence_p WHERE source_id = '" . $source_ID . "' AND project_id = '" . $project_id . "'";
    }else{
	die "cannot find the protein sequence from database based on entered source_ID and project_id" ;
    }


    my $sh = $qh->prepare($sql);
    $sh->execute();
    my $seq = $sh->fetchrow_array();
    $sh->finish();

    my $ua = LWP::UserAgent->new;
    my $args = "CMD=Put&LSet=$LSet&Sequence=" . $seq;
    my $req = new HTTP::Request POST => 'https://mendel.imp.ac.at/gpi/cgi-bin/gpi_pred_fungi.cgi';

    $req->content_type('application/x-www-form-urlencoded');
    $req->content($args);
    my $response = $ua->request($req);

    print $cgi->header("text/html");
    my  $html = "
    <HTML>
	<BODY>
	</BODY>
     </HTML>";

    print $html;


    print $response -> {_content};

	
}



1;

 
