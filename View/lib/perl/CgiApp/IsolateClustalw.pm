package ApiCommonWebsite::View::CgiApp::IsolateClustalw;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use Tie::IxHash;
use ApiCommonWebsite::View::CgiApp;
use Data::Dumper;

use Bio::Graphics::Browser2::PadAlignment;

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  print $cgi->header('text/html');

  $self->processParams($cgi, $dbh);
  $self->handleIsolates($dbh, $cgi);

  exit();
}

sub processParams {
  my ($self, $cgi, $dbh) = @_;
  my $p = $cgi->param('isolate_ids');
  $p =~ s/,$//;
  my @ids = split /,/, $p;
  my $list;
  foreach my $id (@ids){

    $list = $list.  "'" . $id. "',";
  }
  $list =~ s/\,$//;

  $self->{ids} = $list;
}

sub handleIsolates {
  my ($self, $dbh, $cgi) = @_;

  my $ROOT = $ENV{'DOCUMENT_ROOT'};
  my $GUS_HOME = $ENV{'GUS_HOME'};

  my $ids = $self->{ids};

  my $type  = $cgi->param('type');
  my $start = $cgi->param('start');
  my $end   = $cgi->param('end');
  my $sid   = $cgi->param('sid');
  my $project_id = $cgi->param('project_id');

  $start =~ s/,//g;
  $end =~ s/,//g;

  if($end !~  /\d/) {
    $end   = $start + 50;
    $start = $start - 50;
  }
  my $sql = "";

  if($type =~ /htsSNP/i) {
    $ids =~ s/'(\w)/'$sid\.$1/g;
    $ids .= ",'$sid'";   # always compare with reference isolate
    $sql = <<EOSQL;
SELECT source_id, 
       substr(nas.sequence, $start,$end-$start+1) as sequence 
FROM   dots.nasequence nas
WHERE  nas.source_id in ($ids) 
EOSQL
  } else {  # regular isolates
    $sql = <<EOSQL;
SELECT etn.source_id, etn.sequence
FROM   ApidbTuning.IsolateSequence etn
WHERE etn.source_id in ($ids)
EOSQL
  }

  my $sequence;
  my $sth = $dbh->prepare($sql);
  $sth->execute();
  while(my ($id, $seq) = $sth->fetchrow_array()) {
    $id =~ s/^$sid\.// unless ($id eq $sid);
    $sequence .= ">$id\n$seq\n";
  }

  my $range = 10000000;
  my $random = int(rand($range));

  my $infile  = "/tmp/isolate_seq_tmp$random.fas";
  my $outfile = "/tmp/isolate_seq_tmp$random.aln";
  my $dndfile = "/tmp/isolate_seq_tmp$random.dnd";
  my $tmpfile = "/tmp/isolate_seq_tmp$random.tmp";
  
  open(OUT, ">$infile");
  print OUT $sequence;
  close OUT;

  my $cmd = "$GUS_HOME/bin/clustalw2 -infile=$infile -outfile=$outfile > $tmpfile";
  system($cmd);

  open(O, "$outfile");
  my %hash;
  tie %hash, "Tie::IxHash";

  while(<O>) {
    chomp;
    next if (/^CLUSTAL/i || /^\s+$/ || /\*{1,}+/);
    my ($id, $seq) = split /\s+/, $_;
    $id =~ s/\s+//g; 
    next if $id eq ""; # not sure why empty ids are not skipped.
    push @{$hash{$id}}, $seq;
  }
  close O;

  my @dnas;
  my @alignments;
  while(my ($id, $seqs) = each %hash) {
    my $seq = join '', @{$hash{$id}};
    #my $length = length($seq);
    #push @alignments, [$id, 0, $length, 0, $length]; 
    push @dnas, $id, $seq;
  }

  my $align = Bio::Graphics::Browser2::PadAlignment->new(\@dnas,\@alignments);

  my %origins = ();

  print $cgi->pre("CLUSTAL 2.1 Multiple Sequence Alignments\n");
  print $cgi->pre($align->alignment( \%origins, { show_mismatches   => 1,
                                                   show_similarities => 1, 
                                                   show_matches      => 1})); 

     
  open(D, "$dndfile");
  print "<pre>";
  print "<hr>.dnd file\n\n";
  while(<D>) {
    print $_;
  }
  close D;
  print "</pre>";
}

sub error {
  my ($msg) = @_;

  print "ERROR: $msg\n\n";
  exit(1);
}

1;
