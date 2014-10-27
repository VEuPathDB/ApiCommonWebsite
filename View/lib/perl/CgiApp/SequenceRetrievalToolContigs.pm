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

  my ($sourceIds, $starts, $ends, $revComps) = $self->validateParams($cgi, $dbh);

  my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

  my $sql;
  if ($self->{type} eq 'contig') {
    $sql = <<EOSQL;
SELECT s.source_id, s.sequence, ' | ' || sa.sequence_description as description
FROM ApidbTuning.NaSequence s, ApidbTuning.SequenceId si, ApidbTuning.SequenceAttributes sa
WHERE  lower(si.id) = lower(?)
AND s.source_id = si.sequence
AND sa.source_id = s.source_id
EOSQL
  } elsif ($self->{type} eq 'EST') {
    $sql = <<EOSQL;
SELECT ea.source_id, s.sequence, ' | ' || ea.dbest_name as description
FROM ApidbTuning.estAttributes ea,  ApidbTuning.estSequence s
WHERE ea.source_id = s.source_id
and lower(ea.source_id) = lower (?)
EOSQL
} elsif ($self->{type} eq 'Isolate') {
    $sql = <<EOSQL;
SELECT ia.source_id, s.sequence, ' | ' || ia.organism as description
FROM ApidbTuning.IsolateAttributes ia,  ApidbTuning.IsolateSequence s
WHERE ia.source_id = s.source_id
and lower(ia.source_id) = lower (?)
EOSQL
  }

  my $sth = $dbh->prepare($sql);

  my $count = @$sourceIds;

  for (my $i=0; $i < $count; ++$i) {
    $sth->execute(uc($$sourceIds[$i]));
    if (my ($id, $seq, $desc) = $sth->fetchrow_array()) {

      my $bioSeq = Bio::Seq->new(-display_id => $id, -seq => $seq,
				  -alphabet => "dna");
      my $maxEnd = $$ends[$i] > $bioSeq->length()? $bioSeq->length() : $$ends[$i];

      # catch error if start is larger $maxEnd
      &error("Start is larger than the length of the Sequence ($maxEnd)") if ($$starts[$i] > $maxEnd);

      $desc .= " | $$starts[$i] to $maxEnd";
      $desc .= " (reverse-complement)" if ($$revComps[$i]);
      $bioSeq->desc($desc);
      
      $bioSeq = $bioSeq->trunc($$starts[$i], $maxEnd);
      $bioSeq = $bioSeq->revcom() if ($$revComps[$i]);
      $seqIO->write_seq($bioSeq);
    }
  }
  $seqIO->close();

  exit(0);
}

sub validateParams {
  my ($self, $cgi, $dbh) = @_;

  my $ids         = $cgi->param('ids');
  my $start       = $cgi->param('start');
  my $end         = $cgi->param('end');
  my $revComp     = $cgi->param('revComp');

  $start =~ s/[,.+\s]//g if ($start);
  $end =~ s/[,.+\s]//g if ($end);

  $start = 1 if (!$start || $start !~/\S/);
  $end = 100000000 if (!$end || $end !~ /\S/);
  &error("Start '$start' must be a number") unless $start =~ /^\d+$/;
  &error("End '$end' must be a number") unless $end =~ /^\d+$/;
  if ($start < 1 || $end < 1 || $end <= $start) {
      &error("Start and End must be positive, and Start must be less than End (in global parameters)");
  }
  my $type = $self->{type};
  my ($sourceIds, $starts, $ends, $revComps, $type) =  &validateIds($ids, $start, $end, $revComp, $dbh, $type);

  
  return ($sourceIds, $starts, $ends, $revComps);
}

sub validateIds {
  my ($inputIdsString, $start, $end, $revComp, $dbh, $type) = @_;
  # if the input contains commas, assume it comes from WDK:
  #   split on newlines, then split on commas to get rid of site names
  # else if the input contains per-sequence "reverse" or "(start..end)":
  #   split on newlines
  # else split on any whitespace
  my @inputInfo;
  if ($inputIdsString =~ /,/) {
      @inputInfo = split(/\n/, $inputIdsString);
      foreach my $input (@inputInfo) {
	  my @idParts = split(/,/, $input);
	  $input = $idParts[0];
      }
  }
  elsif ($inputIdsString =~ /\d+\.\.\d+\:r/) {
      @inputInfo= split(/\n/, $inputIdsString);
  }
  else {
      @inputInfo = split(/[\s]+/, $inputIdsString);
  }

  my @inputIds;
  my @starts;
  my @ends;
  my @revComps;

  my $sql;
  if ($type eq 'contig') {
  $sql = <<EOSQL;
SELECT s.sequence
FROM ApidbTuning.SequenceId s
WHERE lower(s.id) = lower(?)
EOSQL
} elsif ($type eq 'EST') {
  $sql = <<EOSQL;
SELECT s.sequence
FROM ApidbTuning.estSequence s
WHERE lower(source_id) = lower(?)
EOSQL
} elsif ($type eq 'Isolate') {
  $sql = <<EOSQL;
SELECT s.sequence
FROM ApidbTuning.IsolateSequence s
WHERE lower(source_id) = lower(?)
EOSQL
}
  my @badIds;
  my $sth = $dbh->prepare($sql);
  foreach my $input (@inputInfo) {
   $input =~ s/^\s+//;
   # $input =~s/[\,\s]//g;
    my $inputId;
    if ($input =~ /^(\S+):\d+.*/ || $input =~ /^(\S+)/) {
	$inputId = $1;
	push(@inputIds, $inputId);
    }
    if ($input =~ /.*\:r$/) {
	push(@revComps, 1);
    }
    else {
        push(@revComps, $revComp);
    }
    if ($input =~ /.*\:(\d+)\.\.(\d+)/) {
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

    my $ref = $sth->fetchall_arrayref;
    next if ( $#$ref == 0 );
    push(@badIds, $inputId);
  }
  if (scalar(@badIds) != 0) {
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
