package ApiCommonWebsite::View::CgiApp::SequenceRetrievalToolGenes;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;

my $START = 'Start';
my $END = 'End';

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  print $cgi->header('text/plain');

  $self->initInputId2GeneSourceId($dbh);
  $self->processParams($cgi, $dbh);

  my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

  if ($self->{type} eq 'genomic') {
    $self->handleGenomic($dbh, $seqIO);
  } else {
    $self->handleNonGenomic($dbh, $seqIO);
  }

  $seqIO->close();
  exit();
}

sub initInputId2GeneSourceId {
  my ($self, $dbh) = @_;

  my $sql = <<EOSQL;
SELECT source_id, source_id
FROM   dots.GeneFeature
UNION
SELECT  g.name, gf.source_id
FROM Dots.GeneFeature gf, Dots.NAFeatureNAGene nfng, Dots.NAGene g
WHERE nfng.na_feature_id = gf.na_feature_id
AND nfng.na_gene_id = g.na_gene_id
EOSQL

  my $sth = $dbh->prepare($sql);
  $sth->execute();
  while (my ($alias, $source_id) = $sth->fetchrow_array()) {
    $self->{inputId2GeneSourceId}->{uc($alias)} = $source_id;
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

  # check type
  my @validTypes = ('protein', 'CDS', 'genomic', 'processed_transcript');
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
      unless $self->{inputId2GeneSourceId}->{uc($inputId)};
  }
  &error("Invalid IDs:\n" . join("  \n", @invalidIds))if (scalar(@invalidIds));
}

sub handleNonGenomic {
  my ($self, $dbh, $seqIO) = @_;

  my $sql;
  if ($self->{type} eq "protein") {
    $sql = <<EOSQL;
SELECT tas.sequence, gf.product, tn.name
FROM   dots.translatedaasequence tas, dots.genefeature gf, sres.taxonname tn,
       dots.translatedaafeature taf, dots.transcript t,
     (SELECT na_sequence_id, taxon_id
      FROM dots.ExternalNaSequence
      UNION
      SELECT na_sequence_id, taxon_id
      FROM dots.VirtualSequence) s
WHERE  upper(gf.source_id) = ?
AND t.parent_id = gf.na_feature_id
AND taf.na_feature_id = t.na_feature_id
AND tas.aa_sequence_id = taf.aa_sequence_id
AND gf.na_sequence_id = s.na_sequence_id
AND s.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL
  } elsif ($self->{type} eq "processed_transcript") {
    $sql = <<EOSQL;
SELECT sns.sequence, gf.product, tn.name
FROM dots.SplicedNaSequence sns,  dots.genefeature gf,
     sres.taxonname tn, dots.transcript t,
     (SELECT na_sequence_id, taxon_id
      FROM dots.ExternalNaSequence
      UNION
      SELECT na_sequence_id, taxon_id
      FROM dots.VirtualSequence) s
WHERE upper(gf.source_id) = ?
AND t.parent_id = gf.na_feature_id
AND sns.na_sequence_id = t.na_sequence_id
AND gf.na_sequence_id = s.na_sequence_id
AND s.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL
  } else { # CDS
    $sql = <<EOSQL;
SELECT SUBSTR(s.sequence,
              tf.translation_start,
              tf.translation_stop - tf.translation_start + 1)
         AS sequence,
       gf.product, tn.name
FROM dots.genefeature gf, dots.transcript t,
     sres.taxonname tn, dots.splicednasequence s, dots.TranslatedAaFeature tf
WHERE upper(gf.source_id) = ?
AND t.parent_id = gf.na_feature_id
AND s.na_sequence_id = t.na_sequence_id
AND t.na_feature_id = tf.na_feature_id 
AND s.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL
  }

  my $type = $self->{type};
  my $shortType = $self->{type};
  if ($self->{type} eq 'processed_transcript') {
    $type = 'processed transcript';
    $shortType = 'transcript';
  }

  my $sth = $dbh->prepare($sql);
  for my $inputId (@{$self->{inputIds}}) {
    $sth->execute(uc($inputId));
    my ($seq, $product, $organism) = $sth->fetchrow_array();
    my $geneSourceId = $self->{inputId2GeneSourceId}->{uc($inputId)};
    my $descrip = 
      $self->formatDefline($product, $organism, $geneSourceId, $inputId, 
			   $type, $shortType, "");
    $self->writeSeq($seqIO, $seq, $descrip, $geneSourceId, 1, length($seq), 0);
  }
}

# defline like this:
#    >sourceId description (upstreamAnchor+self->{upstreamOffset} to downstreamAnchor+downstreamOffset) | $self->{type}
sub formatDefline {
  my ($self, $product, $organism, $geneSourceId, $inputGeneId, $type, $shortType, $strand) = @_;

  my $uplus = $self->{upstreamOffset} < 0? "" : "+";
  my $dplus = $self->{downstreamOffset} < 0? "" : "+";

  my $desc = " | $organism | $product | $type | ${strand}(${shortType}$self->{upstreamAnchor}$uplus$self->{upstreamOffset} to ${shortType}$self->{downstreamAnchor}$dplus$self->{downstreamOffset})";

  if ($inputGeneId ne $geneSourceId) {
    $desc = " ($inputGeneId) $desc";
  }
  return $desc;
}

sub handleGenomic {
  my ($self, $dbh, $seqIO) = @_;

  my $seqSourceId2GeneInfoSet = $self->getSeqSourceId2GeneInfoSet($dbh);

  my $sql = <<EOSQL;
SELECT nas.sequence
FROM dots.nasequence nas, 
    (SELECT na_sequence_id, source_id
      FROM dots.ExternalNaSequence 
      UNION
      SELECT na_sequence_id, source_id
      FROM dots.VirtualSequence) s
WHERE  upper(s.source_id) = ?
 AND s.na_sequence_id = nas.na_sequence_id
EOSQL
  my $sth = $dbh->prepare($sql);

  for my $seqSourceId (keys %$seqSourceId2GeneInfoSet) {
    $sth->execute(uc($seqSourceId));
    my ($seq) = $sth->fetchrow_array();
    foreach my $geneInfo (@{$seqSourceId2GeneInfoSet->{$seqSourceId}}) {
      my $descrip = $self->formatGenomicDefline($geneInfo);
      $self->writeSeq($seqIO, $seq, $descrip, $geneInfo->{inputId},
		      $geneInfo->{start}, $geneInfo->{end},
		      $geneInfo->{isReversed});
    }
  }
}

# gather info about each gene in the user's input list, and collate them
# by genomic sequence source id
# return:
#  -a ref to a hash of seqSourceId to set of GeneInfos, one per each gene on the seq
sub getSeqSourceId2GeneInfoSet {
  my ($self, $dbh) = @_;

  my $sql = <<EOSQL;
SELECT s.source_id, tn.name, gf.product, l.start_min, l.end_max, l.is_reversed
FROM dots.GeneFeature gf, dots.NALocation l, sres.taxonname tn,
     (SELECT na_sequence_id, taxon_id, source_id
      FROM dots.ExternalNaSequence
      UNION
      SELECT na_sequence_id, taxon_id, source_id
      FROM dots.VirtualSequence) s
WHERE upper(gf.source_id) = ?
AND s.na_sequence_id = gf.na_sequence_id
AND l.na_feature_id = gf.na_feature_id
AND s.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL

  my $sth = $dbh->prepare($sql);

  my $seqSourceId2GeneInfoSet = {};
  for my $inputId (@{$self->{inputIds}}) {
    my $geneSourceId = $self->{inputId2GeneSourceId}->{uc($inputId)};
    $sth->execute(uc($geneSourceId));
    if (my ($seqSourceId, $organism, $product, $start, $end, $reversed)
	= $sth->fetchrow_array()) {
      my $geneInfo = {geneSourceId => $geneSourceId,
		      inputId => $inputId,
		      start => $start,
		      end => $end,
		      product => $product,
		      organism => $organism,
		      seqSourceId => $seqSourceId,
		      isReversed => $reversed,
		     };
      $seqSourceId2GeneInfoSet->{$seqSourceId} = []
	unless $seqSourceId2GeneInfoSet->{$seqSourceId};
      push(@{$seqSourceId2GeneInfoSet->{$seqSourceId}},$geneInfo);
    } else {
      &error("Can't find genomic sequence for '$inputId'");
    }
  }
  return $seqSourceId2GeneInfoSet;
}

# defline like this:
#    >geneId | inputId | description | seqId forward/reverse upstreamAnchor+upstreamOffset to downstreamAnchor+downstreamOffset | $type 
sub formatGenomicDefline {
  my ($self, $geneInfo) = @_;


  my $strand = "$geneInfo->{seqSourceId} ";
  $strand .= $geneInfo->{isReversed}? 'reverse | ' : 'forward | ';

  my $descrip = $self->formatDefline($geneInfo->{product},
				     $geneInfo->{organism},
				     $geneInfo->{geneSourceId},
				     $geneInfo->{inputId},
				     'genomic',
				     'gene',
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



1;
