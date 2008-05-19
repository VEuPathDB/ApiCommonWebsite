package ApiCommonWebsite::View::CgiApp::SequenceRetrievalToolContigs;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;


sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  my $save = $cgi->param('save');

  if ($save) {
      print $cgi->header('application/x-download');
  }
  else {
      print $cgi->header('text/plain');
  }

  my ($sourceIds, $start, $end, $revComp) = validateParams($cgi, $dbh);

  my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

  my $sql = <<EOSQL;
SELECT s.source_id, nas.sequence, ' | ' || nas.description as description
FROM dots.nasequence nas, 
    (SELECT na_sequence_id, source_id
      FROM dots.ExternalNaSequence 
      UNION
      SELECT na_sequence_id, source_id
      FROM dots.VirtualSequence) s
WHERE  upper(s.source_id) LIKE ?
 AND s.na_sequence_id = nas.na_sequence_id
EOSQL

  my $sth = $dbh->prepare($sql);

  for my $sourceId (@$sourceIds) {
    $sth->execute(uc($sourceId));
    if (my ($id, $seq, $desc) = $sth->fetchrow_array()) {

      $desc .= " | $start to $end";
      $desc .= " (reversed)" if ($revComp);
      my $bioSeq = Bio::Seq->new(-display_id => $id, -seq => $seq,
				 -description => $desc, -alphabet => "dna");
      my $maxEnd = $end > $bioSeq->length()? $bioSeq->length() : $end;

      $bioSeq = $bioSeq->trunc($start, $maxEnd);
      $bioSeq = $bioSeq->revcom() if ($revComp);
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

  my $sourceIds = &validateIds($ids, $dbh);

  $start =~ s/[,.+\s]//g;
  $end =~ s/[,.+\s]//g;

  $start = 1 if (!$start || $start !~/\S/);
  $end = 100000000 if (!$end || $end !~ /\S/);
  &error("Start '$start' must be a number") unless $start =~ /^\d+$/;
  &error("End '$end' must be a number") unless $end =~ /^\d+$/;
  if ($start < 1 || $end < 1 || $end <= $start) {
    &error("Start and End must be positive, and Start must be less than End");
  }
  return ($sourceIds, $start, $end, $revComp);
}

sub validateIds {
  my ($inputIdsString, $dbh) = @_;

  my @inputIds = split(" ", $inputIdsString);

  my $sql = <<EOSQL;
SELECT s.source_id 
FROM (SELECT source_id
      FROM dots.ExternalNaSequence
      UNION
      SELECT source_id
      FROM dots.VirtualSequence) s
WHERE  upper(s.source_id) = ?
EOSQL

  my @badIds;
  my $sth = $dbh->prepare($sql);
  foreach my $inputId (@inputIds) {
    $sth->execute(uc($inputId));
    next if (my ($id) = $sth->fetchrow_array());
    push(@badIds, $inputId);
  }
  if (scalar(@badIds) != 0) {
    my $msg = 
    &error("Invalid IDs:\n" . join("  \n", @badIds));
  }
  return \@inputIds;
}

sub error {
  my ($msg) = @_;

  print "ERROR: $msg\n\n";
  exit(1);
}

1;
