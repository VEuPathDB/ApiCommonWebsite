
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

    my $filename1 = '/var/www/linxu123.plasmodb.org/cgi-bin/test.fa';
    open(FH, '>', $filename1) or die $!;
    print FH $seq;
    close(FH);

    
    my $command = "perl /var/www/linxu123.plasmodb.org/cgi-bin/test.pl --email null\@gmail.com  /var/www/linxu123.plasmodb.org/cgi-bin/test.fa > /var/www/linxu123.plasmodb.org/cgi-bin/myFile.txt";
    my $Interapro_Result  =  `$command`;

    my $jobID;

    my $job_id_file = '/var/www/linxu123.plasmodb.org/cgi-bin/JobID.txt';
    open( FH1, "<$job_id_file" ) or die "Couldn't open file $job_id_file for reading, $!"; 
    while ( my $linedata = <FH1> ) {                                       
	$jobID = $linedata;
    }


    

   # retrieve and display results  
    print "Location: https://www.ebi.ac.uk/interpro/result/InterProScan/$jobID"."\n\n";

}

1;





	
 

