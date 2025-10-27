package ApiCommonWebsite::View::CgiApp::InterPro;
@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use URI::Escape;
use LWP::UserAgent;
use HTTP::Request::Common qw(POST);
use EbrcWebsiteCommon::View::CgiApp;

use CGI;

use File::Temp qw/ tempfile tempdir /;
use Data::Dumper;


$|=1;
print "Content-type: text/html\n\n<head><body>\n<p><b><font size='5' color='DarkCyan'>Your job is currently running...please be patient</font></b>";
my $time = 0;
 while($time < 5)
     {
	 print ' 'x1024;
	 sleep 0.5; # or do something other
	 $time++;
	 }
print "</body></html>\n";


sub run {

    my ($self,$cgi) = @_;

    my $project_id = $cgi->param('project_id');
    my $source_ID = $cgi->param('source_ID');
    my $id_type = $cgi->param('id_type');

    my $qh = $self->getQueryHandle($cgi);

    my $sql;

    if($id_type eq  'protein'){

        $sql = "select SEQUENCE from webready.ProteinSequence where SOURCE_ID = '" . $source_ID . "' and PROJECT_ID= '" . $project_id . "'";
    }else{
       
	die "cannot find the protein sequence from database based on entered source_ID and PROJECT_ID" ;
    }

    my $sh = $qh->prepare($sql);

    $sh->execute();

    my $seq = $sh->fetchrow_array();

    $sh->finish();


=head
    my ($FH, $File) = tempfile(SUFFIX => '.fa');
    print $FH  $seq;
    close ($FH);
=cut

    my ($fh, $file) = tempfile(SUFFIX => '.txt');

####### Download InterPro source code: InterproScan5.pl from  'https://www.ebi.ac.uk/seqdb/confluence/display/JDSAT/InterProScan+5+Help+and+Documentation'


    my $command = "perl InterproScan5.pl  --email null\@gmail.com  $seq &> $file";
    my $Interapro_Result  =  `$command`;

###### We regex $jobID from the outputs that returned from the above command line.

    my $jobID;

    open($fh,"<$file" ) or die "Couldn't open file $file for reading, $!"; 

    my $jobID = <$fh>;
    
    close($fh);

    
    if($jobID =~ /^JobId:\S*\s+(\S+)/){
	$jobID = $1;
    }


    print "<META HTTP-EQUIV=refresh CONTENT=\"1;URL=https://www.ebi.ac.uk/interpro/result/InterProScan/$jobID\">\n";
    #print "Location: https://www.ebi.ac.uk/interpro/result/InterProScan/$jobID</body></html>\n";;         
    #retrieve and display results  
    #print "Location: https://www.ebi.ac.uk/interpro/result/InterProScan/$jobID"."\n\n";

}

1;





	
 
