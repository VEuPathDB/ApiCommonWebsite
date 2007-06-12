package ApiCommonWebsite::View::CgiApp::SequenceRetrievalToolGeneOrf;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;

my $START = 'Start';
my $END = 'End';

##
## NOTE: this SRT makes the icky assumption that there is a 1-1 relationship between genes and 
## proteins, transcripts and CDSs.
##

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  print $cgi->header('text/plain');

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
}

my $geneProteinSql = <<EOSQL;
SELECT gf.source_id, tas.sequence, gf.product, tn.name
FROM   dots.translatedaasequence tas, dots.genefeature gf, sres.taxonname tn,
       dots.translatedaafeature taf, dots.transcript t, apidb.geneid gi
WHERE gi.id = lower(?)
AND gf.source_id = gi.gene
AND t.parent_id = gf.na_feature_id
AND taf.na_feature_id = t.na_feature_id
AND tas.aa_sequence_id = taf.aa_sequence_id
AND tas.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL

my $orfProteinSql = <<EOSQL;
SELECT misc.source_id, tas.sequence,
       tas.description as product,
       tn.name
FROM dots.translatedaasequence tas,
     dots.translatedaafeature taf,
     dots.miscellaneous misc,
     sres.taxonname tn
WHERE misc.source_id = ?
  AND taf.na_feature_id = misc.na_feature_id
  AND tas.aa_sequence_id = taf.aa_sequence_id
  AND tn.taxon_id = tas.taxon_id
  AND tn.name_class = 'scientific name'
EOSQL

my $transcriptSql = <<EOSQL;
SELECT gf.source_id, sns.sequence, gf.product, tn.name
FROM dots.SplicedNaSequence sns,  dots.genefeature gf,
     sres.taxonname tn, dots.transcript t, apidb.geneid gi
WHERE gi.id = lower(?)
AND gf.source_id = gi.gene
AND t.parent_id = gf.na_feature_id
AND sns.na_sequence_id = t.na_sequence_id
AND sns.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL

my $cdsSql = <<EOSQL;
SELECT gf.source_id, SUBSTR(s.sequence,
              tf.translation_start,
              tf.translation_stop - tf.translation_start + 1)
         AS sequence,
       gf.product, tn.name
FROM dots.genefeature gf, dots.transcript t, apidb.geneid gi,
     sres.taxonname tn, dots.splicednasequence s, dots.TranslatedAaFeature tf
WHERE gi.id = lower(?)
AND gf.source_id = gi.gene
AND t.parent_id = gf.na_feature_id
AND s.na_sequence_id = t.na_sequence_id
AND t.na_feature_id = tf.na_feature_id 
AND s.taxon_id = tn.taxon_id
AND tn.name_class = 'scientific name'
EOSQL


sub handleNonGenomic {
  my ($self, $dbh, $seqIO) = @_;

  my $sql;
  my $type = $self->{type};
  if ($type eq "protein") {
      $sql = $self->{geneOrOrf} eq 'gene'? $geneProteinSql : $orfProteinSql;
  } elsif ($type eq "processed_transcript") {
      $sql = $transcriptSql;
  } else { # CDS
      $sql = $cdsSql;
  }
  if ($type eq 'processed_transcript') {
    $type = 'processed transcript';
  }

  my $sth = $dbh->prepare($sql);
  for my $inputId (@{$self->{inputIds}}) {
    $sth->execute($inputId);
    my ($geneOrfSourceId, $seq, $product, $organism) = $sth->fetchrow_array();
    my $descrip = " | $organism | $product | $type ";

    if ($inputId ne $geneOrfSourceId) {
      $descrip = " ($inputId) $descrip";
    }
    $self->writeSeq($seqIO, $seq, $descrip, $geneOrfSourceId, 1, length($seq), 0);
  }
}

sub handleGenomic {
  my ($self, $dbh, $seqIO) = @_;

  my $beginAnch = $self->{upstreamAnchor} eq $START? 'start_min' : 'end_min';
  my $endAnch = $self->{downstreamAnchor} eq $START? 'start_min' : 'end_min';
  my $beginAnchRev = $self->{upstreamAnchor} eq $START? 'end_min' : 'start_min';
  my $endAnchRev = $self->{downstreamAnchor} eq $START? 'end_min' : 'start_min';
  my $beginOffset = $self->{upstreamOffset};
  my $endOffset = $self->{downstreamOffset};

  my $start = "least(l.$beginAnch + $beginOffset, l.$endAnch + $endOffset)";
  my $end = "greatest(l.$beginAnch + $beginOffset, l.$endAnch + $endOffset)";
  my $startRev = "least(l.$beginAnchRev - $beginOffset, l.$endAnchRev - $endOffset)";
  my $endRev = "greatest(l.$beginAnchRev - $beginOffset, l.$endAnchRev - $endOffset)";

  my $geneGenomicSql = <<EOSQL;
select gf.source_id, s.source_id, tn.name, gf.product, l.start_min, l.end_max, l.is_reversed, 
     CASE WHEN l.is_reversed = 1
     THEN substr(s.sequence, $startRev, ($endRev - $startRev + 1))
     ELSE substr(s.sequence, $start, ($end - $start + 1))
     END as sequence
FROM dots.genefeature gf, dots.nalocation l, apidb.geneid gi,
     sres.taxonname tn, dots.externalNaSequence s
WHERE gi.id = lower(?)
AND gf.source_id = gi.gene
AND l.na_feature_id = gf.na_feature_id
AND s.na_sequence_id = gf.na_sequence_id
AND tn.taxon_id = s.taxon_id
AND tn.name_class = 'scientific name'
EOSQL

  my $orfGenomicSql = <<EOSQL;
select misc.source_id, s.source_id, tn.name, '', l.start_min, l.end_max, l.is_reversed, 
     CASE WHEN l.is_reversed = 1
     THEN substr(s.sequence, $startRev, ($endRev - $startRev + 1))
     ELSE substr(s.sequence, $start, ($end - $start + 1))
     END as sequence
FROM dots.miscellaneous misc, dots.nalocation l,
     sres.taxonname tn, dots.externalNaSequence s
WHERE misc.source_id = ?
AND l.na_feature_id = misc.na_feature_id
AND s.na_sequence_id = misc.na_sequence_id
AND tn.taxon_id = s.taxon_id
AND tn.name_class = 'scientific name'

EOSQL

  my $sql;
  if ($self->{geneOrOrf} eq "gene") {
      $sql = $geneGenomicSql;
  } else {
      $sql = $orfGenomicSql;
  }

#     (SELECT na_sequence_id, taxon_id, source_id, sequence
 #     FROM dots.ExternalNaSequence 
 #     UNION
 #     SELECT na_sequence_id, taxon_id, source_id, sequence
 #     FROM dots.VirtualSequence) s


  my @invalidIds;
  my $sth = $dbh->prepare($sql);

  foreach my $inputId (@{$self->{inputIds}}) {
    $sth->execute($inputId);
    my ($geneOrfSourceId, $seqSourceId, $taxonName, $product, $start, $end, $isReversed, $seq)
      = $sth->fetchrow_array();
    if (!$geneOrfSourceId) {
      push(@invalidIds, $inputId);
    } else {
      my $strand = "$seqSourceId " . ($isReversed? 'reverse | ' : 'forward | ');

      my $uplus = $self->{upstreamOffset} < 0? "" : "+";
      my $dplus = $self->{downstreamOffset} < 0? "" : "+";

      my $desc = " | $taxonName | $product | genomic | ${strand}($self->{geneOrOrf}$self->{upstreamAnchor}$uplus$self->{upstreamOffset} to gene$self->{downstreamAnchor}$dplus$self->{downstreamOffset})";

      $desc = " ($inputId) $desc" if ($inputId ne $geneOrfSourceId);

      $self->writeSeq($seqIO, $seq, $desc, $geneOrfSourceId,
		      $start, $end, $isReversed);
    }
  }
  print "\nInvalid IDs:\n" . join("  \n", @invalidIds) if (scalar(@invalidIds));

}

# start, end, geneStart and geneEnd are always in forward strand coordinates
# START is in the native strand coordinates
sub writeSeq {
  my ($self, $seqIO, $seq, $desc, $displayId, $geneStart, $geneEnd, $isReversed) = @_;

  my $length = length($seq);

  if ($length == 0) {
    print "$displayId $desc | length=0\n\n";
  } else {
    my $bioSeq = Bio::Seq->new(-display_id => $displayId,
			       -seq => $seq,
			       -description => "$desc | length=$length",
			       -alphabet => $self->{type} ne "protein" ?
			       "dna" : "protein");
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
