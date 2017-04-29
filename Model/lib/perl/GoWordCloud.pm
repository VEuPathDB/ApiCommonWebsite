package ApiCommonWebsite::Model::GoWordCloud;
use CBIL::Util::Utils qw/ runCmd/; 
#use ApiCommonWebsite::Model::AbstractEnrichment;
#@ISA = (ApiCommonWebsite::Model::AbstractEnrichment);

use strict;
use File::Basename;
use File::Temp qw/ tempfile /; 

sub new {
    my ($class)  = @_;
    print "getting to new\n";
    my $self = {};
    bless( $self, $class );
    return $self;
}

sub run {
    print "getting to run\n";
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
#	foreach my $element (@temps) {
#	    print "$count\t$element\n";
#	    $count++;
#	}

	
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
	    print "to print is $toprint";
	    print "$temps[1]\t$temps[$toprint]\n";

	}	  
	
    }
    print "tmp file is $rfh count is $count\n";
    &runCmd("GoSumWordCloud.r $rFile $outputFile");
}



sub usage {
  my $this = basename($0);

  die "
Produce a wordCloud corresponding to Go Enrichment anaylsis results 
Usage: $this inputFile

where : 

inputFile is the outPut file from the GoEnrichment.pm module
";
}


1;
