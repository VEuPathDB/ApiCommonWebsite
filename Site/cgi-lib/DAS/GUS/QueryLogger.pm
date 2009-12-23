package  DAS::GUS::QueryLogger;

use Time::HiRes qw ( time );
use Fcntl qw(:flock SEEK_END);
use File::Path;

# Log gbrowse queries.
# file locking is commented out, as it is probably overkill and a bit dangerous.

sub new {
    my ($class, $logFileDirectory) = @_;
    my $self = {};

    if ($logFileDirectory) {
      mkpath($logFileDirectory);
      my $logFile = "$logFileDirectory/log1";
      open($self->{logHandle},">> $logFile") ||
	die "Can't open gbrowse log file '$logFile'\n";
    }
    bless($self,$class);
    return $self;
}

# execute a query, and log if we have a handle
sub execute {
    my ($self, $sth, $sql, $moduleName, $queryName, $range, $inGenePage) = @_;
    my $start_time = time();
    my $status = $sth->execute();

    if ($self->{logHandle}) {
      my $elapsed_time = time() - $start_time;
      my $slow = .1;
      if ($range) {
	$slow = $range/10000 * .01;
	$slow = .05 if $slow < .05;
      } else {
	$range = "n/a";
      }
      if ($elapsed_time > $slow) {
        my $fh = $self->{logHandle};
#        lock($fh);
	print $fh "============================================================================\n";
	print $fh "QUERYTIME\t" . localtime() . "\t$moduleName\t" . sprintf("%.2f", $elapsed_time) . "\t$range\t$queryName\n";
	print $fh "============================================================================\n";
	print $fh "$sql\n\n";
#	unlock($fh);
      }
    }
    return $status;
}

sub lock {
  my ($fh) = @_;
  flock($fh, LOCK_EX) or die "Cannot lock gbrowse log - $!\n";

  # and, in case someone appended while we were waiting...
  seek($fh, 0, SEEK_END) or die "Cannot seek - $!\n";
}

sub unlock {
  my ($fh) = @_;
  flock($fh, LOCK_UN) or die "Cannot unlock gbrowse log - $!\n";
}

1;
