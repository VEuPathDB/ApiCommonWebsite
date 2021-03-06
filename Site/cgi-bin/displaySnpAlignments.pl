#!/usr/bin/perl

use strict;
use CGI;
use CGI::Carp qw(fatalsToBrowser set_message);
use GUS::ObjRelP::DbiDatabase;
use WDK::Model::ModelConfig;

BEGIN {
    # Carp callback for sending fatal messages to browser
    sub handle_errors {
        my $msg = shift;
        print "<h3>Oops</h3>";
        print "<p>Got an error: <pre>$msg</pre>";
    }
    set_message(\&handle_errors);
}

my $q = new CGI;
$q->param('project_id') or die "valid 'project_id' is required";

my $snpId = $q->param('snpId') || $ARGV[0];
my $width = $q->param('width') || $ARGV[1]; # width of window on each side of SNP

my $c = new WDK::Model::ModelConfig( $q->param('project_id') );
my $db = new GUS::ObjRelP::DbiDatabase(
    $c->getAppDbDbiDsn,
    $c->getAppDbLogin,
    $c->getAppDbPassword,
    0,1,undef,'core');
my $dbh = $db->getQueryHandle(0);


my @strains = ("3D7", "106_1", "7G8", "D10", "D6", "Dd2", "FCB", "FCC-2", "FCR3", "HB3", "K1", "Malayan", "RO-33", "SantaLucia", "Senegal3101", "Senegal3404", "Senegal3504", "Senegal5102", "V1_S");

my ($seqSrcId, $snpLocation) = getParams($snpId);
my %is_snpLocation = getSnpLocations($seqSrcId, $snpLocation, $width);
my $srcId;

print "Content-type: text/html\n\n";
print '<pre>';

my $bool = 0;  # boolean to check if there is at least one alignment
my $chrNum = $seqSrcId;
$chrNum =~s/Pf3D7_//;  # get chromosome number
$chrNum =~s/0(\d)/$1/; # removing the preceding 0 from chr 1 to 9

foreach my $str (sort @strains) {
  $srcId = $str ."." . $chrNum;  # as '3D7.5' for example

  my $sql = "select source_id,substr(sequence,$snpLocation-$width,2*$width) from dots.nasequence where source_id = '$srcId'";
  my $stmt = $dbh->prepareAndExecute($sql);

  my %seqs;
  while(my ($sourceId,$sequence) = $stmt->fetchrow_array()) {
    $seqs{$sourceId} = $sequence;
  }

  foreach my $s (keys %seqs){
    my @bases = split('', $seqs{$s});

    if ($seqs{$s}=~/[ACGT]/) {
      $bool++;
      for(my $i = 0; $i <$width*2; $i++) {
        if ($i == $width) {
	  $bases[$i] = "<b><font color=\"#FF1800\">$bases[$i]</font></b>";   # bold, and red for SNP in question
	} elsif ($is_snpLocation{$i+$snpLocation-$width} && $bases[$i]=~ /[ACGT]/ ) {
	  $bases[$i] = "<font color=\"#FF1800\">$bases[$i]</font>";   # color red for SNP positions
	}
	print $bases[$i];
      }
      print "  $str<BR>";
    }
  }

}
print '</pre>';
if ($bool) {
  print '<BR> <p><b>Note:</b> This is not a true sequence alignment. Click &#8220;Get Alignment&#8221; below to view a complete alignment.  This display is generated by mapping high quality sequence calls for each strain against the 3D7 genome.  Thus, the * characters could result from the lack of high enough quality or missing sequence or a gap in the relevant genome.  Gaps in 3D7 are not indicated at all.<p>';
} else {
  print 'None';
}


# returns chromosome number and snp location
sub getParams{
  my ($snpSrcId) = @_;
  my $sql = "select pfl.seq_source_id, pfl.old_location from apidb.PlasmoPfalLocations pfl, ApidbTuning.SnpAttributes sa where sa.source_id ='$snpSrcId' and sa.start_min=pfl.new_location and pfl.seq_source_id=sa.seq_source_id";

  my $stmt = $dbh->prepareAndExecute($sql);
  my ($srcId,$start) = $stmt->fetchrow_array();

  return ($srcId, $start);
}

# returns all SNP locations
sub getSnpLocations{
  my ($srcId, $start, $width) = @_;
  my @locations;

  my $sql = "select distinct(old_location) from apidb.PlasmoPfalLocations pfl, ApidbTuning.SnpAttributes sa where sa.seq_source_id='$srcId' and pfl.seq_source_id=sa.seq_source_id and start_min=new_location and old_location > ($start-$width) and old_location < ($start+$width) order by old_location";

  my $stmt = $dbh->prepareAndExecute($sql);
  while(my ($loc) = $stmt->fetchrow_array()){
    push(@locations,$loc);
  }
  #inverting array for speedy lookup
  my %is_snpLocation = ();
  for (@locations) { $is_snpLocation{$_} = 1; }

  return (%is_snpLocation);
}
