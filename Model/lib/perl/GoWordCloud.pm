package ApiCommonWebsite::Model::GoWordCloud;
use CBIL::Util::Utils qw/ runCmd/; 
#use ApiCommonWebsite::Model::AbstractEnrichment;
#@ISA = (ApiCommonWebsite::Model::AbstractEnrichment);

use strict;
use File::Basename;
use File::Temp qw/ tempfile /; 

sub new {
    my ($class)  = @_;

    my $self = {};
    bless( $self, $class );
    return $self;
}

sub run {

    my ($self, $inputFile, $outputFile) = @_;
    my ($rfh, $rFile) = tempfile();
    
    open IN, $inputFile or die "cant open input file $inputFile for reading";
    my $count = 0;
    my $toprint;

    while (my $line = <IN>) {
	$count++;
	chomp $line;
	my @temps = split "\t", $line;
	my $othercount=0;
	
	if (($temps[8]=~/P-value/) || ($temps[8]=~/P-Value/)) {
	    print $rfh $temps[1]."\tPvalue\n";
	    $toprint=8;
	}
	elsif(($temps[9]=~/P-Value/) || ($temps[9]=~/P-value/)){
	    print $rfh $temps[1]."\tPvalue\n";
            $toprint=9;
	}
	else{
	    print $rfh $temps[1]."\t".$temps[$toprint]."\n";
	    #print STDERR "to print is $toprint ";
	    #print STDERR "$temps[1]\t$temps[$toprint]\n";

	}	  
	
    }
    my $cmd = "GoSumWordCloud.r $rFile $outputFile";
    print STDERR "tmp file is $rFile; count is $count; command is $cmd\n";
    &runCmd($cmd);
}


1;
