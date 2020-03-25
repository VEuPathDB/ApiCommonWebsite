
package ApiCommonWebsite::View::CgiApp::InterPro;
@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use URI::Escape;
use LWP::UserAgent;
use HTTP::Request::Common qw(POST);
use EbrcWebsiteCommon::View::CgiApp;


use File::Temp qw/ tempfile tempdir /;



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


    my ($FH, $File) = tempfile(SUFFIX => '.fa');
    print $FH  $seq;
    close ($FH);

    my ($fh, $file) = tempfile(SUFFIX => '.txt');

####### Download InterPro source code: InterproScan5.pl from  'https://www.ebi.ac.uk/seqdb/confluence/display/JDSAT/InterProScan+5+Help+and+Documentation'

    my $command = "perl /var/www/linxu123.plasmodb.org/project_home/ApiCommonWebsite/Site/cgi-bin/InterproScan5.pl  --email null\@gmail.com  $File &> $file";
    my $Interapro_Result  =  `$command`;

###### We regex $jobID from the outputs that returned from the above command line.

    my $jobID;

    open($fh,"<$file" ) or die "Couldn't open file $file for reading, $!"; 

    my $jobID = <$fh>;
    
    close($fh);

    
    if($jobID =~ /^JobId:\S*\s+(\S+)/){
	$jobID = $1;
    }


   # retrieve and display results  
    print "Location: https://www.ebi.ac.uk/interpro/result/InterProScan/$jobID"."\n\n";

}

1;





	
 
