package ApiCommonWebsite::View::CgiApp::SequenceRetrievalToolGeneOrf;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;

my $CODESTART = 'CodeStart';
my $START = 'Start';
my $END = 'End';
my $CODEEND = 'CodeEnd';

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

  my $type = $cgi->param('downloadType');

  if ($type && $type eq "text") {
      print $cgi->header('application/x-download');
  }
  else {
      print $cgi->header('text/plain');
  }

  $self->processParams($cgi, $dbh);

  my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

  if ($self->{type} eq 'genomic') {
    $self->handleGenomic($dbh, $seqIO);
  } else {
    $self->handleNonGenomic($cgi, $dbh, $seqIO);
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
  $self->{upstreamSign}     = $cgi->param('upstreamSign');
  $self->{downstreamSign}   = $cgi->param('downstreamSign');

  # to allow for NOT mapping an id to the latest one
  $self->{ignore_gene_alias}= $cgi->param('ignore_gene_alias');

  my $projectId = $cgi->param('project_id'); 

  # ToxoDB does have automated models anymore. so no special case needed for it
  #  $self->{ignore_gene_alias}= 1 if ($projectId eq 'ToxoDB' || $projectId eq 'EuPathDB');


  my @inputIds;
  foreach(split(/[,\s]+/, $cgi->param('ids'))) {
    push(@inputIds, $_) unless($_ eq $projectId);
  }


  $self->{inputIds}         = \@inputIds;

  $self->{type} = 'protein' if (!$self->{type} || $self->{type} !~ /\S/);

  $self->{upstreamOffset}   =~ s/[,.\s+]//g;
  $self->{downstreamOffset} =~ s/[,.\s+]//g;
  
  $self->{upstreamOffset} = ($self->{upstreamSign} eq 'minus') ? -$self->{upstreamOffset} : $self->{upstreamOffset};
  $self->{downstreamOffset} = ($self->{downstreamSign} eq 'minus') ? -$self->{downstreamOffset} : $self->{downstreamOffset};
  
  # check type
  my @validTypes = ('protein', 'CDS', 'genomic', 'processed_transcript');
  &error("'$self->{type}' is an invalid type") 
    unless grep {$self->{type} eq $_} @validTypes;

  # check anchors
  my @validAnchors = ($START, $CODESTART, $CODEEND, $END);
  #my @validAnchors = ($START, $END);
  &error("'$self->{upstreamAnchor}' is an invalid anchor")
    unless grep {$self->{upstreamAnchor} eq $_} @validAnchors;
  &error("'$self->{downstreamAnchor}' is an invalid anchor")
    unless grep {$self->{downstreamAnchor} eq $_} @validAnchors;
  &error("Illegal anchor combination: stop before start")
      if ((($self->{upstreamAnchor} eq $END || $self->{upstreamAnchor} eq $CODEEND) && ($self->{downstreamAnchor} eq $START || $self->{downstreamAnchor} eq $CODESTART)) || ($self->{upstreamAnchor} eq $CODESTART && $self->{downstreamAnchor} eq $START) || ($self->{upstreamAnchor} eq $END && $self->{downstreamAnchor} eq $CODEEND));
      #if ($self->{upstreamAnchor} eq $END && $self->{downstreamAnchor} eq $START);

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

my $sqlQueries;

$sqlQueries->{geneProteinSql} = <<EOSQL;
SELECT bfmv.source_id, seq.sequence, bfmv.product, bfmv.organism as name
FROM   ApidbTuning.GeneAttributes bfmv, ApidbTuning.ProteinSequence seq
WHERE  bfmv.source_id = seq.source_id
AND    bfmv.source_id = ?
EOSQL

$sqlQueries->{orfProteinSql} = <<EOSQL;
SELECT bfmv.source_id, seq.sequence,
       CASE bfmv.is_reversed
	   WHEN 0 THEN 'nt ' || bfmv.start_min || '-' || bfmv.end_max || ' of ' || bfmv.nas_id
	   ELSE 'nt ' || bfmv.end_max || '-' || bfmv.start_min || ' of ' || bfmv.nas_id 
       END as product,
       bfmv.organism as name
FROM   ApidbTuning.OrfAttributes bfmv, ApidbTuning.OrfSequence seq
WHERE  bfmv.source_id = seq.source_id
AND    bfmv.source_id = ?
EOSQL

$sqlQueries->{transcriptSql} = <<EOSQL;
SELECT bfmv.source_id, seq.sequence, bfmv.product, bfmv.organism as name
FROM   ApidbTuning.GeneAttributes bfmv, ApidbTuning.TranscriptSequence seq
WHERE  bfmv.source_id = seq.source_id
AND    bfmv.source_id = ?
EOSQL

$sqlQueries->{cdsSql} = <<EOSQL;
SELECT bfmv.source_id, seq.sequence, bfmv.product, bfmv.organism as name
FROM   ApidbTuning.GeneAttributes bfmv, ApidbTuning.CodingSequence seq
WHERE  bfmv.source_id = seq.source_id
AND    bfmv.source_id = ?
EOSQL



sub handleNonGenomic {
  my ($self, $cgi, $dbh, $seqIO) = @_;

  my $sql;
  my $type = $self->{type};
  my $site = $sqlQueries;

  my $inputIds = $self->{inputIds};
  my $ids;

  if ($self->{ignore_gene_alias}) {
    $ids = $inputIds;
  } else {
    $ids = $self->mapGeneFeatureSourceIds($inputIds, $dbh);
  }

  if($type eq "protein" && $self->{geneOrOrf} eq 'gene') {
    $sql = $site->{geneProteinSql}

  }
  # use the input ids directly
  elsif($type eq "protein" && $self->{geneOrOrf} ne 'gene') {
    $sql = $site->{orfProteinSql};
    $ids = $inputIds;
  } 
  elsif ($type eq "processed_transcript") {
    $sql = $site->{transcriptSql};
    $type = 'processed transcript';
  } 
  else { # CDS
    $sql = $site->{cdsSql};
  }

  &error("No id provided could be mapped to valid source ids") unless(scalar @$ids > 0);


  my @invalidIds;
  my $sth = $dbh->prepare($sql);
  for my $inputId (@$ids) {
    $sth->execute($inputId);
    my ($geneOrfSourceId, $seq, $product, $organism) = $sth->fetchrow_array();
    my $descrip = " | $organism | $product | $type ";

    if ($inputId ne $geneOrfSourceId) {
      push(@invalidIds, $inputId);
    } else {
      $self->writeSeq($seqIO, $seq, $descrip, $geneOrfSourceId, 1, length($seq), 0);
    }
  }
  print "\nInvalid IDs:\n" . join("  \n", @invalidIds) if (scalar(@invalidIds));

}

sub mapGeneFeatureSourceIds {
  my ($self, $inputIds, $dbh) = @_;

  my $sh = $dbh->prepare("select gene from (select gene, case when id = gene then 2 when id = lower(gene) then 1 else 0 end as matchiness from ApidbTuning.GeneId where lower(id) = lower(?) order by matchiness desc) where rownum=1");

  my @ids;

  foreach my $in (@{$inputIds}) {
    $sh->execute($in);

    my $best;
    while(my ($sourceId) = $sh->fetchrow_array()) {
      $best = $sourceId;
    }
    push @ids, $best if($best);
  }

  $sh->finish();

  return \@ids;
}

sub handleGenomic {
  my ($self, $dbh, $seqIO) = @_;

  my $site = $sqlQueries;

  my $beginAnch = 0;
  my $endAnch = 0;
  my $beginAnchRev = 0;
  my $endAnchRev = 0;

  $beginAnch = $self->{upstreamAnchor} eq $START ? 'bfmv.start_min' : $self->{upstreamAnchor} eq $END ? 'bfmv.end_max' : $self->{upstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_start,bfmv.start_min)' : 'nvl(bfmv.coding_end,bfmv.end_max)';
  $endAnch = $self->{downstreamAnchor} eq $START ? 'bfmv.start_min' : $self->{downstreamAnchor} eq $END ? 'bfmv.end_max' : $self->{downstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_start,bfmv.start_min)' : 'nvl(bfmv.coding_end,bfmv.end_max)';
  $beginAnchRev = $self->{upstreamAnchor} eq $START ? 'bfmv.end_max' : $self->{upstreamAnchor} eq $END ? 'bfmv.start_min' : $self->{upstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_start,bfmv.end_max)' : 'nvl(bfmv.coding_end,bfmv.start_min)';
  $endAnchRev = $self->{downstreamAnchor} eq $START ? 'bfmv.end_max' : $self->{downstreamAnchor} eq $END ? 'bfmv.start_min' : $self->{downstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_start,bfmv.end_max)' : 'nvl(bfmv.coding_end,bfmv.start_min)';

  my $beginOffset = $self->{upstreamOffset};
  my $endOffset = $self->{downstreamOffset};

  my $start = "";
  my $end = "";
  my $startRev = "";
  my $endRev = "";
  
  $start = "($beginAnch + $beginOffset)";
  $end = "($endAnch + $endOffset)";
  $startRev = "($endAnchRev - $endOffset)";
  $endRev = "($beginAnchRev - $beginOffset)";

$sqlQueries->{geneGenomicSql} = <<EOSQL;
select bfmv.source_id, s.source_id, bfmv.organism, bfmv.product,
     bfmv.start_min, bfmv.end_max,
     DECODE(bfmv.strand,'reverse',1,'forward',0) as is_reversed,
     CASE WHEN bfmv.is_reversed = 1
     THEN $beginAnchRev
     ELSE $beginAnch END as expect_start,
     CASE WHEN bfmv.is_reversed = 1
     THEN $endAnchRev
     ELSE $endAnch END as expect_end,
     CASE WHEN bfmv.strand = 'reverse'
     THEN
       CASE WHEN $startRev < 0
       THEN substr(s.sequence, 0, greatest(0, ($endRev + 1)))
       ELSE substr(s.sequence, $startRev, greatest(0, ($endRev - $startRev + 1)))
       END
     ELSE
       CASE WHEN $start < 0
       THEN substr(s.sequence, 0, greatest(0, ($end + 1)))
       ELSE substr(s.sequence, $start, greatest(0, ($end - $start + 1)))
       END
     END as sequence
FROM ApidbTuning.GeneAttributes bfmv, ApidbTuning.GeneId gi,
     ApidbTuning.NaSequence s
WHERE lower(gi.id) = lower(?)
AND bfmv.source_id = gi.gene
AND s.source_id = bfmv.sequence_id
EOSQL

$sqlQueries->{orfGenomicSql} = <<EOSQL;
select bfmv.source_id, s.source_id, bfmv.organism, 
     CASE WHEN bfmv.is_reversed = 0
     THEN 'nt ' || bfmv.start_min || '-' || bfmv.end_max || ' of ' || bfmv.nas_id
     ELSE 'nt ' || bfmv.end_max || '-' || bfmv.start_min || ' of ' || bfmv.nas_id 
     END as product,
     bfmv.start_min, bfmv.end_max, 
     bfmv.is_reversed,
     CASE WHEN bfmv.is_reversed = 1
     THEN $beginAnchRev
     ELSE $beginAnch END as expect_start,
     CASE WHEN bfmv.is_reversed = 1
     THEN $endAnchRev
     ELSE $endAnch END as expect_end,
     CASE WHEN bfmv.is_reversed = 1
     THEN 
       CASE WHEN $startRev < 0
       THEN substr(s.sequence, 0, greatest(0, ($endRev + 1)))
       ELSE substr(s.sequence, $startRev, greatest(0, ($endRev - $startRev + 1)))
       END
     ELSE
       CASE WHEN $start < 0
       THEN substr(s.sequence, 0, greatest(0, ($end + 1)))
       ELSE substr(s.sequence, $start, greatest(0, ($end - $start + 1)))
       END
     END as sequence
FROM ApidbTuning.OrfAttributes bfmv, ApidbTuning.NaSequence s
WHERE bfmv.source_id = ?
AND s.source_id = bfmv.nas_id
EOSQL

  my $sql;

  my $ids = $self->{inputIds};

  if ($self->{geneOrOrf} eq "gene") {
      $sql = $site->{geneGenomicSql};
      $ids = $self->mapGeneFeatureSourceIds($ids, $dbh) unless($self->getModel() =~ /^eupath/i);
  } else {
      $sql = $site->{orfGenomicSql};
  }

  &error("No id provided could be mapped to valid source ids") unless(scalar @$ids > 0);

  my @invalidIds;
  my $sth = $dbh->prepare($sql) or &error($DBI::errstr);

  foreach my $inputId (@$ids) {
    $sth->execute($inputId);
    my ($geneOrfSourceId, $seqSourceId, $taxonName, $product, $start, $end, $isReversed, $expectStart, $expectEnd, $seq)
      = $sth->fetchrow_array() ;
    if (!$geneOrfSourceId) {
      push(@invalidIds, $inputId);
    } else {
       if ($isReversed == 0) {
	   $expectStart = $expectStart + $beginOffset;
	   $expectEnd = $expectEnd + $endOffset; 
       }
       else {
	   $expectStart = $expectStart - $endOffset;
	   $expectEnd = $expectEnd - $beginOffset;
       }
      my $expectedLength = $expectEnd - $expectStart  + 1;

      my $strand = "$seqSourceId " . ($isReversed? 'reverse | ' : 'forward | ');
      my $uplus = $self->{upstreamOffset} < 0? "" : "+";
      my $dplus = $self->{downstreamOffset} < 0? "" : "+";
      my $model = $self->getModel();
      my $desc = " | $taxonName | $product | genomic | ${strand}($self->{geneOrOrf}$self->{upstreamAnchor}$uplus$self->{upstreamOffset} to $self->{geneOrOrf}$self->{downstreamAnchor}$dplus$self->{downstreamOffset})";
      if (length($seq) < $expectedLength ) {
	  $desc = $desc . " | WARNING: Partial sequence retrieved";
      }
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

