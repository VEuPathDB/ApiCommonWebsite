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

  while (my $line = <IN>) {
      chomp $line;
      my @temps = split "\t", $line;
      print $rfh $temps[1]."\t".$temps[3]."\n";
  }
#  &runCmd("GoSumWordCloud.r $rFile $outputFile");
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
