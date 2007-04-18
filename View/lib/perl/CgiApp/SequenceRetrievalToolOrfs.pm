package CryptoDBWebsite::View::CgiApp::SequenceRetrievalToolOrfs;

@ISA = qw( CryptoDBWebsite::View::CgiApp );

use strict;
use CryptoDBWebsite::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;

my $START = 'Start';
my $END = 'End';

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  print $cgi->header('text/plain');

  $self->initInputId2OrfSourceId($dbh);
  $self->processParams($cgi, $dbh);

  my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

  if ($self->{type} eq 'genomic') {
    $self->handleGenomic($dbh, $seqIO);
  } else {
    $self->handleProtein($dbh, $seqIO);
  }

  $seqIO->close();
  exit();
}

sub initInputId2OrfSourceId {
  my ($self, $dbh) = @_;

  my $sql = <<EOSQL;
SELECT source_id, source_id
FROM   dots.translatedaasequence
EOSQL

  my $sth = $dbh->prepare($sql);
  $sth->execute();
  while (my ($alias, $source_id) = $sth->fetchrow_array()) {
    $self->{inputId2OrfSourceId}->{uc($alias)} = $source_id;
  }

}

sub processParams {
  my ($self, $cgi, $dbh) = @_;

  $self->{type}             = $cgi->param('type');
  $self->{upstreamOffset}   = $cgi->param('upstreamOffset');
  $self->{downstreamOffset} = $cgi->param('downstreamOffset');
  $self->{upstreamAnchor}   = $cgi->param('upstreamAnchor');
  $self->{downstreamAnchor} = $cgi->param('downstreamAnchor');
  my @inputIds              = split(" ", $cgi->param('ids'));
  $self->{inputIds}         = \@inputIds;

  $self->{type} = 'protein' if (!$self->{type} || $self->{type} !~ /\S/);

  $self->{upstreamOffset}   =~ s/[,.\s+]//g;
  $self->{downstreamOffset} =~ s/[,.\s+]//g;

  # check type
  my @validTypes = ('protein', 'genomic');
  &error("'$self->{type}' is an invalid type") 
    unless grep {$self->{type} eq $_} @validTypes;

  # check anchors
  my @validAnchors = ($START, $END);
  &error("'$self->{upstreamAnchor}' is an invalid anchor")
    unless grep {$self->{upstreamAnchor} eq $_} @validAnchors;
  &error("'$self->{downstreamAnchor}' is an invalid anchor")
    unless grep {$self->{downstreamAnchor} eq $_} @validAnchors;
  &error("Illegal anchor combination: stop before start")
    if ($self->{upstreamAnchor} eq $END && $self->{downstreamAnchor} eq $START);

  # check offsets
  $self->{upstreamOffset} = 0
    if (!$self->{upstreamOffset} || $self->{upstreamOffset} !~/\S/);
  $self->{downstreamOffset} = 0
    if (!$self->{downstreamOffset} || $self->{downstreamOffset} !~ /\S/);
  &error("UpstreamOffset '$self->{upstreamOffset}' must be a number")
    unless $self->{upstreamOffset} =~ /^-?\d+$/;
  &error("DownstreamOffset '$self->{downstreamOffset}' must be a number")
    unless $self->{downstreamOffset} =~ /^-?\d+$/;

  # check input IDs
  my @invalidIds;
  foreach my $inputId (@{$self->{inputIds}}) {
    push(@invalidIds, $inputId)
      unless $self->{inputId2OrfSourceId}->{uc($inputId)};
  }
  &error("Invalid IDs:\n" . join("  \n", @invalidIds))if (scalar(@invalidIds));
}

sub handleProtein {
  my ($self, $dbh, $seqIO) = @_;

  my $sql;
    $sql = <<EOSQL;
SELECT tas.sequence,
       tas.description as product,
       tn.name
FROM dots.translatedaasequence tas,
     dots.translatedaafeature taf,
     dots.transcript t,
     sres.taxonname tn, 
     dots.externalnasequence enas
WHERE taf.aa_sequence_id = tas.aa_sequence_id
AND t.na_feature_id = taf.na_feature_id
AND t.na_sequence_id = enas.na_sequence_id
AND enas.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
AND upper(tas.source_id) = ?
EOSQL

  my $type = $self->{type};
  my $shortType = $self->{type};


  my $sth = $dbh->prepare($sql);
  for my $inputId (@{$self->{inputIds}}) {
    $sth->execute(uc($inputId));
    my ($seq, $product, $organism) = $sth->fetchrow_array();
    $product = $self->id2product($inputId) unless $product;
    my $orfSourceId = $self->{inputId2OrfSourceId}->{uc($inputId)};
    my $descrip = 
      $self->formatDefline($product, $organism, $orfSourceId, $inputId, 
			   $type, $shortType, "");
    $self->writeSeq($seqIO, $seq, $descrip, $orfSourceId, 1, length($seq), 0);
  }
}

# defline like this:
#    >sourceId description (upstreamAnchor+self->{upstreamOffset} to downstreamAnchor+downstreamOffset) | $self->{type}
sub formatDefline {
  my ($self, $product, $organism, $orfSourceId, $inputOrfId, $type, $shortType, $strand) = @_;

  my $uplus = $self->{upstreamOffset} < 0? "" : "+";
  my $dplus = $self->{downstreamOffset} < 0? "" : "+";

  my $desc = " | $organism | $product | $type | ${strand}(${shortType}$self->{upstreamAnchor}$uplus$self->{upstreamOffset} to ${shortType}$self->{downstreamAnchor}$dplus$self->{downstreamOffset})";

  if ($inputOrfId ne $orfSourceId) {
    $desc = " ($inputOrfId) $desc";
  }
  return $desc;
}

sub handleGenomic {
  my ($self, $dbh, $seqIO) = @_;

  my $seqSourceId2OrfInfoSet = $self->getSeqSourceId2OrfInfoSet($dbh);

  my $sql = <<EOSQL;
SELECT sequence
FROM   dots.externalnasequence
WHERE  upper(source_id) = ?
EOSQL
  my $sth = $dbh->prepare($sql);

  for my $seqSourceId (keys %$seqSourceId2OrfInfoSet) {
    $sth->execute(uc($seqSourceId));
    my ($seq) = $sth->fetchrow_array();
    foreach my $orfInfo (@{$seqSourceId2OrfInfoSet->{$seqSourceId}}) {
      my $descrip = $self->formatGenomicDefline($orfInfo);
      $self->writeSeq($seqIO, $seq, $descrip, $orfInfo->{inputId},
		      $orfInfo->{start}, $orfInfo->{end},
		      $orfInfo->{isReversed});
    }
  }
}

# gather info about each orf in the user's input list, and collate them
# by genomic sequence source id
# return:
#  -a ref to a hash of seqSourceId to set of OrfInfos, one per each orf on the seq
sub getSeqSourceId2OrfInfoSet {
  my ($self, $dbh) = @_;

  my $sql = <<EOSQL;
select enas.source_id, tn.name, tas.description,
nal.start_max, nal.end_min, nal.is_reversed
from dots.translatedaasequence tas,
dots.translatedaafeature taf,
dots.transcript t,
dots.nalocation nal,
dots.externalnasequence enas,
sres.taxonname tn
where taf.aa_sequence_id = tas.aa_sequence_id
and t.na_feature_id = taf.na_feature_id
and t.na_sequence_id = enas.na_sequence_id
and taf.na_feature_id = nal.na_feature_id
and tas.source_id = ?
AND enas.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL

  my $sth = $dbh->prepare($sql);

  my $seqSourceId2OrfInfoSet = {};
  for my $inputId (@{$self->{inputIds}}) {
    my $orfSourceId = $self->{inputId2OrfSourceId}->{uc($inputId)};
    $sth->execute(uc($orfSourceId));
    if (my ($seqSourceId, $organism, $product, $start, $end, $reversed)
	  = $sth->fetchrow_array()) {
      $product = $self->id2product($inputId) unless $product;
      my $orfInfo = {orfSourceId => $orfSourceId,
		      inputId => $inputId,
		      start => $start,
		      end => $end,
		      product => $product,
		      organism => $organism,
		      seqSourceId => $seqSourceId,
		      isReversed => $reversed,
		     };
      $seqSourceId2OrfInfoSet->{$seqSourceId} = []
	unless $seqSourceId2OrfInfoSet->{$seqSourceId};
      push(@{$seqSourceId2OrfInfoSet->{$seqSourceId}},$orfInfo);
    } else {
      &error("Can't find genomic sequence for '$inputId'");
    }
  }
  return $seqSourceId2OrfInfoSet;
}

# defline like this:
#    >orfId | inputId | description | seqId forward/reverse upstreamAnchor+upstreamOffset to downstreamAnchor+downstreamOffset | $type 
sub formatGenomicDefline {
  my ($self, $orfInfo) = @_;


  my $strand = "$orfInfo->{seqSourceId} ";
  $strand .= $orfInfo->{isReversed}? 'reverse | ' : 'forward | ';

  my $descrip = $self->formatDefline($orfInfo->{product},
				     $orfInfo->{organism},
				     $orfInfo->{orfSourceId},
				     $orfInfo->{inputId},
				     'genomic',
				     'orf',
				     $strand,
				    );
  return $descrip;
}

# start, end, geneStart and geneEnd are always in forward strand coordinates
# START is in the native strand coordinates
sub writeSeq {
  my ($self, $seqIO, $seq, $desc, $displayId, $geneStart, $geneEnd, $isReversed) = @_;

  my ($start,$end, $upstream, $downstream);

  if ($isReversed) {
    $upstream = $self->{upstreamAnchor} eq $START? $geneEnd : $geneStart;

    $downstream = $self->{downstreamAnchor} eq $START? $geneEnd : $geneStart;

    $upstream -= $self->{upstreamOffset};

    $downstream -= $self->{downstreamOffset};

    $start = $downstream;

    $end = $upstream;
  } else {
    $upstream = $self->{upstreamAnchor} eq $START? $geneStart : $geneEnd;

    $downstream = $self->{downstreamAnchor} eq $START? $geneStart : $geneEnd;

    $upstream += $self->{upstreamOffset};

    $downstream += $self->{downstreamOffset};

    $start = $upstream;

    $end = $downstream;

  }

  $start = $start < 1? 1 : $start;

  $end = $end > length($seq)? length($seq) : $end;

  if ($start > $end) {
    print "$displayId $desc | length=0\n\n";
  } else {
    my $length = $end - $start +1;
    my $bioSeq = Bio::Seq->new(-display_id => $displayId,
			       -seq => $seq,
			       -description => "$desc | length=$length",
			       -alphabet => $self->{type} ne "protein" ?
			       "dna" : "protein");
    $bioSeq = $bioSeq->trunc($start, $end);

    $bioSeq = $bioSeq->revcom() if $isReversed;

    $seqIO->write_seq($bioSeq);
  }
}


sub error {
  my ($msg) = @_;

  print "ERROR: $msg\n\n";
  exit(1);
}

sub id2product {
    my ($self, $id) = @_;
    my ($seqid, $frame, $start, $end) = split /-/, $id;
    return "nt $start-$end of $seqid";
}


1;
