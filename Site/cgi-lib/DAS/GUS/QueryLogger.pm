package  DAS::GUS::QueryLogger;

use Time::HiRes qw ( time );
use Fcntl qw(:flock SEEK_END);
use File::Path;

# Log gbrowse queries.
# file locking is commented out, as it is probably overkill and a bit dangerous.
my $LOG10 = log(10);

# hard coded configuration
my $RANGEMIN = 50000;
my $SLIGHT = 0.05;
my $MEDIUM = 0.5;
my $VERY =   2.0;

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
      open($self->{slightlySlowHandle},">> $logFile") ||
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

      my $reportedRange = $range? $range : "n/a";
      my $r = ($range && $range >= $RANGEMIN)? $range : $RANGEMIN;

      # scaling here so that 50k = 1, 1m = 5 and 5m = 9
      # in other words, an approx 10 fold difference in speed allowed
      # across typical smallest window (gene page) and largest (chromosome)
      my $scaledRange = &log10($r/5000) ** 2;
      $slightlySlow = $scaledRange * $SLIGHT;
      $mediumSlow = $scaledRange * $MEDIUM;
      $verySlow = $scaledRange * $VERY;

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
	print $fh "QUERYTIME\t" . localtime() . "\t" . time() . "\t$howSlow\t$moduleName\t" . sprintf("%.2f", $elapsed_time) . "\t$reportedRange\t$queryName\n";
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

sub log10 {
  my $n = shift;
  return log($n)/$LOG10;
}

1;
