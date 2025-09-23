package ApiCommonWebsite::View::CgiApp::SequenceRetrievalToolGeneOrf;

@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use EbrcWebsiteCommon::View::CgiApp;

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

  $self->setSqls();

  my $seqIO;
  if ($self->{noLineBreaks}) {
    $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta', -width=>"32766");
  } else {
    $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');
  }

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

  $self->{startOffset3}   = $cgi->param('startOffset3');
  $self->{endOffset3} = $cgi->param('endOffset3');
  $self->{startAnchor3}   = $cgi->param('startAnchor3');
  $self->{endAnchor3} = $cgi->param('endAnchor3');
  $self->{sourceIdFilter} = $cgi->param('sourceIdFilter');
  $self->{onlyIdDefLine} = $cgi->param('onlyIdDefLine');
  $self->{noLineBreaks} = $cgi->param('noLineBreaks');


  # to allow for NOT mapping an id to the latest one
  $self->{ignore_gene_alias}= $cgi->param('ignore_gene_alias');

  my $projectId = $cgi->param('project_id'); 

  # ToxoDB does have automated models anymore. so no special case needed for it
  #  $self->{ignore_gene_alias}= 1 if ($projectId eq 'ToxoDB' || $projectId eq 'EuPathDB');

#  print STDERR "IN processParams : ". $cgi->param('ids') ."\n";
  my @allIds;
  my @inputIds; # unique IDs
  foreach(split(/[,\s]+/, $cgi->param('ids'))) {
    push(@allIds, $_) unless($_ eq $projectId);
    my %hash   = map { $_ => 1 } @allIds;
    @inputIds = keys %hash;
  }


  $self->{inputIds}         = \@inputIds;

  $self->{type} = 'protein' if (!$self->{type} || $self->{type} !~ /\S/);

  $self->{upstreamOffset}   =~ s/[,.\s+]//g;
  $self->{downstreamOffset} =~ s/[,.\s+]//g;

  $self->{upstreamOffset} = ($self->{upstreamSign} eq 'minus') ? -$self->{upstreamOffset} : $self->{upstreamOffset};
  $self->{downstreamOffset} = ($self->{downstreamSign} eq 'minus') ? -$self->{downstreamOffset} : $self->{downstreamOffset};

  $self->{startOffset3}   =~ s/[,.\s+]//g;
  $self->{endOffset3} =~ s/[,.\s+]//g;


  # check type
  my @validTypes = ('protein', 'CDS', 'genomic', 'processed_transcript');
  &error("'$self->{type}' is an invalid type") 
    unless grep {$self->{type} eq $_} @validTypes;

  if($self->{type} eq 'genomic') {
    # check anchors
    my @validAnchors = ($START, $CODESTART, $CODEEND, $END);
    #my @validAnchors = ($START, $END);
    &error("'$self->{upstreamAnchor}' is an invalid upstream anchor")
        unless grep {$self->{upstreamAnchor} eq $_} @validAnchors;
    &error("'$self->{downstreamAnchor}' is an invalid downstream anchor")
        unless grep {$self->{downstreamAnchor} eq $_} @validAnchors;
    &error("Illegal anchor combination: stop before start")
        if ((($self->{upstreamAnchor} eq $END || $self->{upstreamAnchor} eq $CODEEND) && ($self->{downstreamAnchor} eq $START || $self->{downstreamAnchor} eq $CODESTART)) || ($self->{upstreamAnchor} eq $CODESTART && $self->{downstreamAnchor} eq $START) || ($self->{upstreamAnchor} eq $END && $self->{downstreamAnchor} eq $CODEEND));
      #if ($self->{upstreamAnchor} eq $END && $self->{downstreamAnchor} eq $START);

#  &error("'$self->{startAnchor3}' is an invalid anchor")
#    unless grep {$self->{startAnchor3} eq $_} @validAnchors;
#  &error("'$self->{endAnchor3}' is an invalid anchor")
#    unless grep {$self->{endAnchor3} eq $_} @validAnchors;
    &error("Illegal anchor combination: stop before start codest andcodeend: $START AND $END ")
        if ((($self->{startAnchor3} eq $END || $self->{startAnchor3} eq $CODEEND) && ($self->{endAnchor3} eq $START || $self->{endAnchor3} eq $CODESTART)) || ($self->{startAnchor3} eq $CODESTART && $self->{endAnchor3} eq $START) || ($self->{startAnchor3} eq $END && $self->{downstreamAnchor3} eq $CODEEND));
  }

  else {
    # check offsets
    $self->{upstreamOffset} = 0
        if (!$self->{upstreamOffset} || $self->{upstreamOffset} !~/\S/);
    $self->{downstreamOffset} = 0
        if (!$self->{downstreamOffset} || $self->{downstreamOffset} !~ /\S/);
    &error("UpstreamOffset '$self->{upstreamOffset}' must be a number")
        unless $self->{upstreamOffset} =~ /^-?\d+$/;
    &error("DownstreamOffset '$self->{downstreamOffset}' must be a number")
        unless $self->{downstreamOffset} =~ /^-?\d+$/;

    $self->{startOffset3} = 0
        if (!$self->{startOffset3} || $self->{startOffset3} !~/\S/);
    $self->{endOffset3} = 0
        if (!$self->{endOffset3} || $self->{endOffset3} !~ /\S/);
    &error("UpstreamOffset '$self->{startOffset3}' must be a number")
        unless $self->{startOffset3} =~ /^-?\d+$/;
    &error("DownstreamOffset '$self->{endOffset3}' must be a number")
        unless $self->{endOffset3} =~ /^-?\d+$/;
  }

}

sub setSqls {
  my ($self) = @_;

  my $sourceIdAndClause = "(bfmv.gene_source_id = ? OR bfmv.source_id = ?)";
  $sourceIdAndClause = "bfmv.representative_transcript =  bfmv.source_id and bfmv.gene_source_id = ?" if $self->{sourceIdFilter} eq 'genesOnly';
  $sourceIdAndClause = "bfmv.gene_source_id = ?" if $self->{sourceIdFilter} eq 'transcriptsOnly';
  my $sourceId = $self->{sourceIdFilter} eq 'genesOnly'? "bfmv.gene_source_id" : "bfmv.source_id";

  $self->{sqlQueries}->{geneProteinSql} = <<EOSQL;
SELECT $sourceId, seq.sequence, bfmv.gene_product AS product, bfmv.organism AS name
FROM   webready.TranscriptAttributes bfmv, webready.ProteinSequence seq
WHERE  bfmv.protein_source_id = seq.source_id
AND    $sourceIdAndClause
ORDER BY bfmv.source_id
EOSQL

  $self->{sqlQueries}->{orfProteinSql} = <<EOSQL;
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

  $self->{sqlQueries}->{transcriptSql} = <<EOSQL;
SELECT $sourceId, seq.sequence, bfmv.gene_product AS product, bfmv.organism AS name
FROM   webready.TranscriptAttributes bfmv, webready.TranscriptSequence seq
WHERE  bfmv.source_id = seq.source_id
AND    $sourceIdAndClause
ORDER BY bfmv.source_id
EOSQL

  $self->{sqlQueries}->{cdsSql} = <<EOSQL;
SELECT $sourceId, seq.sequence, bfmv.gene_product AS product, bfmv.organism AS name
FROM   webready.TranscriptAttributes bfmv, webready.CodingSequence seq
WHERE  bfmv.source_id = seq.source_id
AND    $sourceIdAndClause
ORDER BY bfmv.source_id
EOSQL

}


sub handleNonGenomic {
  my ($self, $cgi, $dbh, $seqIO) = @_;

  my $sql;
  my $type = $self->{type};
  my $site = $self->{sqlQueries};

  my $inputIds = $self->{inputIds};
  my $ids;

  if ($self->{ignore_gene_alias}) {
    $ids = $inputIds;
  } else {
    $ids = $self->mapGeneFeatureSourceIds($inputIds, $dbh);
  }

  if($type eq "protein" && $self->{geneOrOrf} eq 'gene') {

    my ($start_position, $end_position, $seq_length);
    $start_position = $self->{startOffset3};
    my $protLen = "bfmv.protein_length";
    my $add = $self->{startOffset3} + $self->{endOffset3};

    $start_position = 1 if ($self->{startOffset3} ==0);

    if ($self->{startAnchor3} eq $END && $self->{endAnchor3} eq $END) {
      # when both Start and End are in terms of 'downstream from Stop'
      $start_position = "$protLen - $self->{startOffset3}";
      $seq_length = "$self->{startOffset3} - $self->{endOffset3}  + 1";
    } elsif ($self->{startAnchor3} eq $END) {
      # when Start is in terms of 'downstream from Stop'
      $start_position = "$protLen - $self->{startOffset3}";
      $seq_length = "$add - $protLen + 1";
    } elsif ($self->{endAnchor3} eq $END) {
      # when End is in terms of 'downstream from Stop'
      $seq_length = "$protLen + 1 - $add ";
    } else {
      # when both Start and End are in terms of 'upstream from Start'
      $seq_length = "$self->{endOffset3} - $self->{startOffset3} + 1";
    }

    my $sourceIdAndClause = "(bfmv.gene_source_id = ? OR bfmv.source_id = ?)";
    $sourceIdAndClause = "bfmv.representative_transcript =  bfmv.source_id and bfmv.gene_source_id = ?" if $self->{sourceIdFilter} eq 'genesOnly';
    $sourceIdAndClause = "bfmv.gene_source_id = ?" if $self->{sourceIdFilter} eq 'transcriptsOnly';
    my $sourceId = $self->{sourceIdFilter} eq 'genesOnly'? "bfmv.gene_source_id" : "bfmv.source_id";


    $self->{sqlQueries}->{geneProteinSql} = <<EOSQL;
SELECT $sourceId, substr(seq.sequence,  $start_position, ($seq_length)),
        bfmv.gene_product AS product, bfmv.organism AS name
FROM   webready.TranscriptAttributes bfmv, webready.ProteinSequence seq
WHERE  bfmv.protein_source_id = seq.source_id
AND    $sourceIdAndClause
ORDER BY bfmv.source_id
EOSQL
    $sql = $site->{geneProteinSql};
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
    if ($self->{sourceIdFilter}) {
      $sth->execute($inputId);  # if filter applied, use only one ID, not two
    } else {
      $sth->execute($inputId, $inputId);
    }


   while (my ($geneOrfSourceId, $seq, $product, $organism) = $sth->fetchrow_array()){
    my $descrip = " | $organism | $product | $type ";

    if (!$geneOrfSourceId) {  # NOTE: i don't see how we could ever get this condition, as the sql always returns a src id
 #  if ($inputId ne $geneOrfSourceId) {
      push(@invalidIds, $inputId);
    } else {
      $descrip = " ($inputId) $descrip" if ($inputId ne $geneOrfSourceId); # don't get here if $self->{sourceIdFilter} is set.
      $self->writeSeq($seqIO, $seq, $descrip, $geneOrfSourceId, 1, length($seq), 0);
    }
  }
  print "\nInvalid IDs:\n" . join("  \n", @invalidIds) if (scalar(@invalidIds));
 }
}

sub mapGeneFeatureSourceIds {
  my ($self, $inputIds, $dbh) = @_;

  my $sql = <<SQL;
      select gene as id
      from (select gene,
                   case
                     when id = gene
                       then 2
                     when id = lower(gene)
                       then 1
                     else 0
                   end as matchiness
            from webready.GeneId
            where lower(id) = lower(?)
            order by matchiness desc)
      where rownum=1
    UNION
      select source_id as id
      from webready.TranscriptAttributes
      where lower(source_id) = lower(?)
SQL

  my $sh = $dbh->prepare($sql);
  my @ids;

  foreach my $in (@{$inputIds}) {
    $sh->execute($in, $in);

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

  my $site = $self->{sqlQueries};

  my $beginAnch = 0;
  my $endAnch = 0;
  my $beginAnchRev = 0;
  my $endAnchRev = 0;

  $beginAnch = $self->{upstreamAnchor} eq $START ? 'bfmv.start_min' : $self->{upstreamAnchor} eq $END ? 'bfmv.end_max' : $self->{upstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_start,bfmv.start_min)' : 'nvl(bfmv.coding_end,bfmv.end_max)';
  $endAnch = $self->{downstreamAnchor} eq $START ? 'bfmv.start_min' : $self->{downstreamAnchor} eq $END ? 'bfmv.end_max' : $self->{downstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_start,bfmv.start_min)' : 'nvl(bfmv.coding_end,bfmv.end_max)';
  $beginAnchRev = $self->{upstreamAnchor} eq $START ? 'bfmv.end_max' : $self->{upstreamAnchor} eq $END ? 'bfmv.start_min' : $self->{upstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_end,bfmv.end_max)' : 'nvl(bfmv.coding_start,bfmv.start_min)';
  $endAnchRev = $self->{downstreamAnchor} eq $START ? 'bfmv.end_max' : $self->{downstreamAnchor} eq $END ? 'bfmv.start_min' : $self->{downstreamAnchor} eq $CODESTART ? 'nvl(bfmv.coding_end,bfmv.end_max)' : 'nvl(bfmv.coding_start,bfmv.start_min)';


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

  $self->{sqlQueries}->{geneGenomicSql} = <<EOSQL;
select bfmv.gene_source_id, s.source_id, bfmv.organism, bfmv.gene_product as product,
     bfmv.start_min, bfmv.end_max,
     bfmv.is_reversed,
     CASE WHEN bfmv.is_reversed = 1
     THEN $beginAnchRev
     ELSE $beginAnch END as expect_start,
     CASE WHEN bfmv.is_reversed = 1
     THEN $endAnchRev
     ELSE $endAnch END as expect_end,
     CASE WHEN bfmv.is_reversed =1
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
FROM webready.TranscriptAttributes bfmv, webready.GenomicSequenceSequence s
WHERE s.source_id = bfmv.sequence_id
AND bfmv.gene_source_id IN (
    SELECT gene FROM (
        SELECT gene, CASE WHEN id = gene THEN 2 WHEN id = LOWER(gene) THEN 1 ELSE 0 END AS matchiness
        FROM webready.GeneId WHERE LOWER(id) = LOWER( ?)
        ORDER BY matchiness desc )
    WHERE rownum=1 )
ORDER BY bfmv.gene_source_id
EOSQL

  $self->{sqlQueries}->{orfGenomicSql} = <<EOSQL;
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
FROM ApidbTuning.OrfAttributes bfmv, webready.GenomicSequenceSequence s
WHERE bfmv.source_id = ?
AND s.source_id = bfmv.nas_id
ORDER BY bfmv.source_id
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
      = $sth->fetchrow_array();

    if (!$geneOrfSourceId) {
      push(@invalidIds, $inputId) unless $self->{sourceIdFilter} eq 'genesOnly';
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
  if ($self->{onlyIdDefLine}){
    $desc = '';
  } else {
    $desc = $desc . "| length=" . $length;
  }

  if ($length == 0) {
    print "$displayId $desc | length=0\n\n";
  } else {

    my $bioSeq = Bio::Seq->new(-display_id => $displayId,
			       -seq => $seq,
			       -description => $desc,
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

