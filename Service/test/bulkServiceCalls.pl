use strict;

usage() unless scalar(@ARGV) == 1;

my ($inputLogFile) = @ARGV;

my $cmd = 'curl -i -X POST -H "Content-Type: application/json"  http://sfischer.plasmodb.org/plasmo.sfischer/service/answer -d @tmpFile';

open(LF, $inputLogFile) || die "Can't open input log file '$inputLogFile'\n";

my $body;
my $answerRequest;
while(<LF>) {
  if (/HTTP/) {
    if ($body) {
      open(T, ">tmpFile") || die "Can't open output file\n";
      print T $body . "\n";
      close(T);
      print STDERR $cmd . "\n";
      print STDERR "$body\n";
      system($cmd) && die "Failed running cmd\n";
      sleep(1);
      $body = "";
    }
    $answerRequest = /answer/;
  } elsif ($answerRequest) {
    s/Request Body://;
    $body .= $_;
  }
}


sub usage {
  die "

Run service calls specified in input service log file.

Usage: bulkServiceCalls service_log_file
";

}
