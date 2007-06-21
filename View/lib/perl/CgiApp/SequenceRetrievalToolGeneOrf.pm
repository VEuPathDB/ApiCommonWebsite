package ApiCommonWebsite::View::CgiApp::SequenceRetrievalToolGeneOrf;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;

my $START = 'Start';
my $END = 'End';

##
## PROBLEM NOTE: this SRT makes the icky assumption that there is a 1-1 relationship between genes and 
## proteins, transcripts and CDSs.
##
## PROBLEM NOTE: the ORF queries are not robust:  they assume that source_id is unique in 
## dots.miscellaneous and that the translation of the orf has the same source_id
##
## PROBLEM NOTE: the queries for the portal also rely on dubious source_id joins
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

my $componentSql;
my $portalSql;

$componentSql->{geneProteinSql} = <<EOSQL;
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

$componentSql->{orfProteinSql} = <<EOSQL;
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

$componentSql->{transcriptSql} = <<EOSQL;
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

$componentSql->{cdsSql} = <<EOSQL;
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

$portalSql->{geneProteinSql} = <<EOSQL;
SELECT bfmv.source_id, tas.sequence, bfmv.product, bfmv.organism
FROM   dots.translatedaasequence tas, apidb.geneattributes bfmv, apidb.geneid gi
WHERE gi.id = lower(?)
AND bfmv.source_id = gi.gene
AND tas.source_id = bfmv.source_id
EOSQL

$portalSql->{orfProteinSql} = <<EOSQL;
SELECT  bfmv.source_id, tas.sequence,
       CASE bfmv.is_reversed 
	   WHEN 0 THEN 'nt ' || bfmv.start_min || '-' || bfmv.end_max || ' of ' || bfmv.nas_id
	   ELSE 'nt ' || bfmv.end_max || '-' || bfmv.start_min || ' of ' || bfmv.nas_id 
       END as product,
       bfmv.organism
FROM apidb.orfattributes bfmv, dots.translatedaasequence tas
WHERE tas.source_id = ?
AND bfmv.source_id = tas.source_id
EOSQL

$portalSql->{transcriptSql} = <<EOSQL;
SELECT bfmv.source_id, sns.sequence, bfmv.product, bfmv.organism
FROM dots.splicednasequence sns, apidb.geneattributes bfmv, 
     sres.sequenceontology so, apidb.geneid gi
WHERE gi.id = lower(?)
AND bfmv.source_id = gi.gene
AND sns.source_id = bfmv.source_id
AND so.sequence_ontology_id = sns.sequence_ontology_id
AND so.term_name = 'processed_transcript'
EOSQL

$portalSql->{cdsSql} = <<EOSQL;
SELECT bfmv.source_id, s.sequence, bfmv.product, bfmv.organism
FROM apidb.geneattributes bfmv, dots.splicednasequence s, sres.sequenceontology so,
     apidb.geneid gi
WHERE gi.id = lower(?)
AND bfmv.source_id = gi.gene
AND s.source_id = bfmv.source_id
AND so.sequence_ontology_id = s.sequence_ontology_id
AND so.term_name = 'CDS'
EOSQL



sub handleNonGenomic {
  my ($self, $dbh, $seqIO) = @_;

  my $sql;
  my $type = $self->{type};
  my $site = ($self->getModel() =~ /api/)? $portalSql : $componentSql;


  if ($type eq "protein") {
      $sql = $self->{geneOrOrf} eq 'gene'?
	$site->{geneProteinSql} : $site->{orfProteinSql};
  } elsif ($type eq "processed_transcript") {
      $sql = $site->{transcriptSql};
  } else { # CDS
      $sql = $site->{cdsSql};
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

  my $seqTable = ($self->getModel() =~ /toxo/)?
    'dots.VirtualSequence' : 'dots.ExternalNaSequence';

  my $beginAnch = 0;
  my $endAnch = 0;
  my $beginAnchRev = 0;
  my $endAnchRev = 0;

# Comment out if using componentSql
#if($self->getModel() =~ /api/) {
     $beginAnch = $self->{upstreamAnchor} eq $START? 'start_min' : 'end_max';
     $endAnch = $self->{downstreamAnchor} eq $START? 'start_min' : 'end_max';
     $beginAnchRev = $self->{upstreamAnchor} eq $START? 'end_max' : 'start_min';
     $endAnchRev = $self->{downstreamAnchor} eq $START? 'end_max' : 'start_min';
#}
#  else {
#      $beginAnch = $self->{upstreamAnchor} eq $START? 'start_min' : 'end_min';
#      $endAnch = $self->{downstreamAnchor} eq $START? 'start_min' : 'end_min';
#      $beginAnchRev = $self->{upstreamAnchor} eq $START? 'end_min' : 'start_min';
#      $endAnchRev = $self->{downstreamAnchor} eq $START? 'end_min' : 'start_min';
#  }

  my $beginOffset = $self->{upstreamOffset};
  my $endOffset = $self->{downstreamOffset};

  my $start = "";
  my $end = "";
  my $startRev = "";
  my $endRev = "";
  
# Comment out if using componentSql
#if($self->getModel() =~ /api/) {
      $start = "(bfmv.$beginAnch + $beginOffset)";
      $end = "(bfmv.$endAnch + $endOffset)";
      $startRev = "(bfmv.$endAnchRev - $endOffset)";
      $endRev = "(bfmv.$beginAnchRev - $beginOffset)";
# }
# else {
#     $start = "(l.$beginAnch + $beginOffset)";
#     $end = "(l.$endAnch + $endOffset)";
#     $startRev = "(l.$endAnchRev - $endOffset)";
#     $endRev = "(l.$beginAnchRev - $beginOffset)";
# }


$componentSql->{geneGenomicSql} = <<EOSQL;
select gf.source_id, s.source_id, tn.name, gf.product, l.start_min, l.end_max, l.is_reversed, 
     CASE WHEN l.is_reversed = 1
     THEN substr(s.sequence, $startRev, greatest(0, ($endRev - $startRev + 1)))
     ELSE substr(s.sequence, $start, greatest(0, ($end - $start + 1)))
     END as sequence
FROM dots.genefeature gf, dots.nalocation l, apidb.geneid gi,
     sres.taxonname tn, $seqTable s
WHERE gi.id = lower(?)
AND gf.source_id = gi.gene
AND l.na_feature_id = gf.na_feature_id
AND s.na_sequence_id = gf.na_sequence_id
AND tn.taxon_id = s.taxon_id
AND tn.name_class = 'scientific name'
EOSQL

$componentSql->{orfGenomicSql} = <<EOSQL;
select misc.source_id, s.source_id, tn.name, '', l.start_min, l.end_max, l.is_reversed, 
     CASE WHEN l.is_reversed = 1
     THEN substr(s.sequence, $startRev, greatest(0, ($endRev - $startRev + 1)))
     ELSE substr(s.sequence, $start, greatest(0, ($end - $start + 1)))
     END as sequence
FROM dots.miscellaneous misc, dots.nalocation l,
     sres.taxonname tn, $seqTable s
WHERE misc.source_id = ?
AND l.na_feature_id = misc.na_feature_id
AND s.na_sequence_id = misc.na_sequence_id
AND tn.taxon_id = s.taxon_id
AND tn.name_class = 'scientific name'
EOSQL


$portalSql->{geneGenomicSql} = <<EOSQL;
select bfmv.source_id, s.source_id, bfmv.organism, bfmv.product, bfmv.start_min, bfmv.end_max, 
     DECODE(bfmv.strand,'reverse',1,'forward',0) as is_reversed,
     CASE WHEN bfmv.strand = 'reverse'
     THEN substr(s.sequence, $startRev, greatest(0, ($endRev - $startRev + 1)))
     ELSE substr(s.sequence, $start, greatest(0, ($end - $start + 1)))
     END as sequence
FROM apidb.geneattributes bfmv, apidb.geneid gi,
     $seqTable s
WHERE gi.id = lower(?)
AND bfmv.source_id = gi.gene
AND s.source_id = bfmv.sequence_id
EOSQL

$portalSql->{orfGenomicSql} = <<EOSQL;
select bfmv.source_id, s.source_id, bfmv.organism, 
     CASE WHEN bfmv.is_reversed = 0
     THEN 'nt ' || bfmv.start_min || '-' || bfmv.end_max || ' of ' || bfmv.nas_id
     ELSE 'nt ' || bfmv.end_max || '-' || bfmv.start_min || ' of ' || bfmv.nas_id 
     END as product,
     bfmv.start_min, bfmv.end_max, bfmv.is_reversed,
     CASE WHEN bfmv.is_reversed = 1
     THEN substr(s.sequence, $startRev, greatest(0, ($endRev - $startRev + 1)))
     ELSE substr(s.sequence, $start, greatest(0, ($end - $start + 1)))
     END as sequence
FROM apidb.orfattributes bfmv, $seqTable s
WHERE bfmv.source_id = ?
AND s.source_id = bfmv.nas_id
EOSQL

  my $sql;

#  CAN COMPONENT SITES USE PORTAL SQL FOR GENOMIC GENES AND ORFS?
#  my $site = ($self->getModel() =~ /api/)? $portalSql : $componentSql;
  my $site = $portalSql;


  if ($self->{geneOrOrf} eq "gene") {
      $sql = $site->{geneGenomicSql};
  } else {
      $sql = $site->{orfGenomicSql};
  }

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
      my $model = $self->getModel();
      my $desc = " | $taxonName | $product | genomic | ${strand}($self->{geneOrOrf}$self->{upstreamAnchor}$uplus$self->{upstreamOffset} to $self->{geneOrOrf}$self->{downstreamAnchor}$dplus$self->{downstreamOffset})";
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
