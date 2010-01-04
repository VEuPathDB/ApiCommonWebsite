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
      $self->{logFileDir} = $logFileDirectory;
      mkpath($logFileDirectory);
      my $logFile = "$logFileDirectory/very_slow.log";
      open($self->{verySlowHandle},">> $logFile") ||
	die "Can't open gbrowse log file '$logFile'\n";
      $logFile = "$logFileDirectory/medium_slow.log";
      open($self->{mediumSlowHandle},">> $logFile") ||
	die "Can't open gbrowse log file '$logFile'\n";
      $logFile = "$logFileDirectory/slightly_slow.log";
      open($self->{slightySlowHandle},">> $logFile") ||
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

    if ($self->{logFileDir}) {
      my $elapsed_time = time() - $start_time;
      my $slightySlow = .1;
      my $mediumSlow = 1;
      my $verySlow = 2;
      if ($range) {
	my $rangeFloor = $range < 50000? 50000 : $range;
	$verySlow = $range/10000 * .5;       # eg, 2.5 sec for 50k
	$mediumSlow = $range/10000 * .1;     # eg, 0.5 sec for 50k
	$slightlySlow = $range/10000 * .01;  # eg, .05 sec for 50k
      } else {
	$range = "n/a";
      }
      my $fh;
      my $howSlow;
      if ($elapsed_time > $verySlow) {
	$fh = $self->{verySlowHandle};
	$howSlow = 'v';
      } elsif ($elapsed_time > $mediumSlow) {
	$fh = $self->{mediumSlowHandle};
	$howSlow = 'm';
      } elsif ($elapsed_time > $slightlySlow) {
	$fh = $self->{slightlySlowHandle};
	$howSlow = 's';
      }
      if ($fh) {
#        lock($fh);
	print $fh "============================================================================\n";
	print $fh "QUERYTIME\t" . localtime() . "\t" . time() . "\t$howSlow\t$moduleName\t" . sprintf("%.2f", $elapsed_time) . "\t$range\t$queryName\n";
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
