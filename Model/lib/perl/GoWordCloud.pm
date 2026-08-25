package ApiCommonWebsite::Model::GoWordCloud;
use CBIL::Util::Utils qw/ runCmd/; 

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
    my $scoreCol;

    my $line = <IN>;
    my @header = split "\t", $line;

    if ($header[8]=~/Benjamini/) {
      $scoreCol=8;
    } else {
      die "Can't find score column 'Benjamini' in header 9th column: "  . join(", ", @header) . "\n";
    }

    print $rfh $header[1] . "\tPvalue\n";

    while ($line = <IN>) {
	$count++;
	chomp $line;
	my @temps = split "\t", $line;
	print $rfh $temps[1]."\t".$temps[$scoreCol]."\n";
	#print STDERR "$temps[1]\t$temps[$scoreCol]\n";
    }

    my $cmd = "GoSumWordCloud.r $rFile $outputFile";
    print STDERR "tmp file is $rFile; count is $count; command is $cmd\n";
    &runCmd($cmd);
}


1;
