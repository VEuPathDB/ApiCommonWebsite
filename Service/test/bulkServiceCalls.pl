use strict;

usage() unless scalar(@ARGV) == 2;

my ($inputLogFile, $url) = @ARGV;

my $cmd = "curl -i -X POST -H \"Content-Type: application/json\"  $url/service/answer -d \@tmpFile";

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

For now, only recognizes calls to answer service.

Usage: bulkServiceCalls service_log_file url

Where:
  service_log_file:  a wdk service log file.
  url:  part of the url before the /service.  Eg:  http://sfischer.plasmodb.org/plasmo.sfischer
";

}
