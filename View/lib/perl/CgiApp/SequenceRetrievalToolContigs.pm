package ApiCommonWebsite::View::CgiApp::SequenceRetrievalToolContigs;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;


sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  my $type = $cgi->param('downloadType');

  if ($type && $type eq "text") {
      print $cgi->header('application/x-download');
  }
  else {
      print $cgi->header('text/plain');
  }

  my ($sourceIds, $starts, $ends, $revComps) = validateParams($cgi, $dbh);

  my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

  my $sql = <<EOSQL;
SELECT s.source_id, s.sequence, ' | ' || sa.sequence_description as description
FROM dots.nasequence s, apidb.sequenceid si, apidb.sequenceattributes sa
WHERE  si.id = lower(?)
AND s.source_id = si.sequence
AND sa.source_id = s.source_id
EOSQL

  my $sth = $dbh->prepare($sql);

  my $count = @$sourceIds;

  for (my $i=0; $i < $count; ++$i) {
    $sth->execute(uc($$sourceIds[$i]));
    if (my ($id, $seq, $desc) = $sth->fetchrow_array()) {

      $desc .= " | $$starts[$i] to $$ends[$i]";
      $desc .= " (reverse-complement)" if ($$revComps[$i]);
      my $bioSeq = Bio::Seq->new(-display_id => $id, -seq => $seq,
				 -description => $desc, -alphabet => "dna");
      my $maxEnd = $$ends[$i] > $bioSeq->length()? $bioSeq->length() : $$ends[$i];

      $bioSeq = $bioSeq->trunc($$starts[$i], $maxEnd);
      $bioSeq = $bioSeq->revcom() if ($$revComps[$i]);
      $seqIO->write_seq($bioSeq);
    }
  }
  $seqIO->close();

  exit(0);
}

sub validateParams {
  my ($cgi, $dbh) = @_;

  my $ids         = $cgi->param('ids');
  my $start       = $cgi->param('start');
  my $end         = $cgi->param('end');
  my $revComp     = $cgi->param('revComp');

  $start =~ s/[,.+\s]//g;
  $end =~ s/[,.+\s]//g;

  $start = 1 if (!$start || $start !~/\S/);
  $end = 100000000 if (!$end || $end !~ /\S/);
  &error("Start '$start' must be a number") unless $start =~ /^\d+$/;
  &error("End '$end' must be a number") unless $end =~ /^\d+$/;
  if ($start < 1 || $end < 1 || $end <= $start) {
      &error("Start and End must be positive, and Start must be less than End (in global parameters)");
  }
  
  my ($sourceIds, $starts, $ends, $revComps) = &validateIds($ids, $start, $end, $revComp, $dbh);
  
  return ($sourceIds, $starts, $ends, $revComps);
}

sub validateIds {
  my ($inputIdsString, $start, $end, $revComp, $dbh) = @_;
  
  # if the input contains per-sequence "reverse" or "(start..end)"
  # info, then split on newlines; else split on commas or any whitespace
  my @inputInfo = ($inputIdsString =~ /reverse|\(\d+\.\.\d+\)/)?
     split(/\n/, $inputIdsString) : split(/[,\s]+/, $inputIdsString);
  my @inputIds;
  my @starts;
  my @ends;
  my @revComps;

  my $sql = <<EOSQL;
SELECT s.sequence
FROM apidb.sequenceid s
WHERE s.id = lower(?)
EOSQL

  my @badIds;
  my $sth = $dbh->prepare($sql);
  foreach my $input (@inputInfo) {
    my $inputId;
    if ($input =~ /^(\w+\.?\w*)/) {
	$inputId = $1;
	push(@inputIds, $inputId);
    }
    if ($input =~ /reverse/) {
	push(@revComps, 1);
    }
    else {
        push(@revComps, $revComp);
    }
    if ($input =~ /\((\d+)\.\.(\d+)\)/) {
	if ($1 < 1 || $2 < 1 || $2 <= $1) {
	    &error("Start and End must be positive, and Start must be less than End for sequence:  $inputId");
	}
        push(@starts, $1);
        push(@ends, $2);
    }
    else {
        push(@starts, $start);
	push(@ends, $end);
    }
    $sth->execute(uc($inputId));
    next if (my ($id) = $sth->fetchrow_array());
    push(@badIds, $inputId);
  }
  if (scalar(@badIds) != 0) {
    my $msg = 
    &error("Invalid IDs:\n" . join("  \n", @badIds));
  }
  return (\@inputIds, \@starts, \@ends, \@revComps);
}

sub error {
  my ($msg) = @_;

  print "ERROR: $msg\n\n";
  exit(1);
}

1;
