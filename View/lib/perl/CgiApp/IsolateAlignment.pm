package ApiCommonWebsite::View::CgiApp::IsolateAlignment;
@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use Tie::IxHash;
use EbrcWebsiteCommon::View::CgiApp;
use Data::Dumper;
use Bio::Seq;
use LWP::UserAgent;
use HTTP::Request::Common;
use HTTP::Request::Common qw(POST);

use File::Temp qw/ tempfile /;

sub run {
  my ($self, $cgi) = @_;

  my ($ids,$sid,$type,$start,$end,$clustalQueryType,$userOutFormat) = $self->getParams($cgi);

  my $sequences = $self->getSequencesFromDatabase($cgi,$type,$clustalQueryType,$ids,$sid,$start,$end);

  my $inFile = writeSequencesToFile($sequences);

  my ($outFile,$dndFile) = runClustalO($inFile,$userOutFormat);

  my $dndData = getDndData($dndFile);

  my $itolLink = getItolLink($dndData);
     # NOTE: check where else this is used (e.g., SNP etc). This uses the dnd file output.

  &createHTML($itolLink,$outFile,$cgi,$userOutFormat,$dndFile);

  exit();
}


sub getParams {
    my ($self, $cgi) = @_;

    my $type  = $cgi->param('type');
    my $ids = $self->getIds($cgi,$type);
    my ($start,$end) = $self->getStartAndEnd($cgi);
    my $clustalQueryType = $self->getClustalQueryType($cgi);
    my $sid   = $cgi->param('sid');
    my $userOutFormat = $self->getUserOutFormat($cgi);
    return ($ids,$sid,$type,$start,$end,$clustalQueryType,$userOutFormat);
}

sub getUserOutFormat {
    my ($self,$cgi) = @_; 
    my $userOutFormat = $cgi->param('clustalOutFormat');
    if ((! defined $userOutFormat) || ($userOutFormat eq "")){
	$userOutFormat = "clu";
    }
    return $userOutFormat;
}

sub getClustalQueryType {
    my ($self,$cgi) = @_;
    my $clustalQueryType = $cgi->param('sequence_Type');
    if ((! defined $clustalQueryType) || ($clustalQueryType eq "")) {
	$clustalQueryType = "protein";
    }
    return $clustalQueryType;
}

sub getStartAndEnd {
    my ($self,$cgi) = @_;

    my $start = $cgi->param('start');
    $start =~ s/,//g;
    my $end   = $cgi->param('end');
    $end =~ s/,//g;
    if ($end !~ /\d/) {
	$end   = $start + 50;
	$start = $start - 50;
    }
    return ($start,$end);
}

sub getIds {
    my ($self,$cgi,$type) = @_;
    my @idArray;
    if ($type eq 'geneOrthologs') {
	@idArray = $cgi->param('gene_ids');
    } else {
	my $ids = $cgi->param('isolate_ids');
	$ids =~ s/,$//;
	@idArray = split(',', $ids);
    }
    return join(',', map { "'$_'" } @idArray);
}

sub getSql {
    my ($self,$cgi,$dbh,$type,$clustalQueryType,$ids,$sid,$start,$end) = @_;
    my $sql="";
    if ($type =~ /htsSNP/i) {
	$sql = getHtsSnpSql($ids,$sid,$start,$end);
    } elsif ($type eq 'geneOrthologs') {
	$sql = $self->getOrthoSql($cgi,$dbh,$ids,$clustalQueryType);
    } else {  # regular isolates
	$sql = getPopsetSql($ids);
    }
    return $sql;
}

sub getOrthoSql {
    my ($self,$cgi,$dbh,$ids,$clustalQueryType) = @_;
    my $sql="";
    if ($clustalQueryType eq 'protein' ){
	$sql = getOrthoProteinSql($ids);
    } elsif ($clustalQueryType eq 'CDS'){
	$sql = getOrthoCdsSql($ids);
    } elsif ($clustalQueryType eq 'genomic'){
	my ($upstreamOffset,$downstreamOffset) = $self->getOffsets($cgi);
	my $areProteins = proteinTest($dbh,$ids);
	$sql = $areProteins ? getOrthoGenomicIsProteinSql($ids,$upstreamOffset,$downstreamOffset)
	                    : getOrthoGenomicNotProteinSql($ids,$upstreamOffset,$downstreamOffset);
    }
    return $sql;
}

sub getOffsets {
    my ($self,$cgi) = @_;
    my $upstreamOffset = $cgi->param('oneOffset');
    $upstreamOffset = abs($upstreamOffset);
    my $downstreamOffset = $cgi->param('twoOffset');
    $downstreamOffset = abs($downstreamOffset);
    
    # Alters user input if user selects >2500 nt.  
    if ($upstreamOffset > 2500) {$upstreamOffset = 2500;}
    if ($downstreamOffset > 2500) {$downstreamOffset = 2500;}

    return ($upstreamOffset,$downstreamOffset);
}

sub getSequencesFromDatabase {
    my ($self,$cgi,$type,$clustalQueryType,$ids,$sid,$start,$end) = @_;

    my $dbh = $self->getQueryHandle($cgi);
    my $sql = $self->getSql($cgi,$dbh,$type,$clustalQueryType,$ids,$sid,$start,$end);
              # NOTE: sql needs to return array of 3 values. Only the gene page Clustal Omega uses 3 values.
              # Beware the order of values [id, strand, seq]. Strand is only used for the genomic query, so a dummy value is used elsewhere.

    my $sequences = "";
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    while(my ($id, $strand, $seq) = $sth->fetchrow_array()) {
	next if ($seq !~ /[ACGT]/);   # there are no nucleotides
	$seq = reverseComplement($seq) if ($strand eq 'reverse' && $clustalQueryType eq "genomic");
	$id =~ s/^$sid\.// unless ($id eq $sid);    # remove reference gene name for other isolates
	$sequences .= ">$id\n$seq\n";
    }
    return $sequences;
}

sub reverseComplement {
    my ($seq) = @_;
    my $seqR = Bio::Seq->new(-seq => $seq, alphabet => 'dna');
    $seqR = $seqR->revcom();
    $seq = $seqR->seq;
    return $seq;
}

sub writeSequencesToFile {
    my ($sequences) = @_;
    my ($inFh, $inFile)  = tempfile();
    print $inFh $sequences;
    close $inFh;
    return $inFile;
}

sub runClustalO {
    my ($inFile,$userOutFormat) = @_;
    my ($outFh, $outFile) = tempfile();
    my ($dndFh, $dndFile) = tempfile();
    my ($tmpFh, $tmpFile) = tempfile();
    my $cmd = "clustalo -v --residuenumber --infile=$inFile --outfile=$outFile --outfmt=$userOutFormat --output-order=tree-order --guidetree-out=$dndFile --force > $tmpFile";
    system($cmd);
    close $outFh; close $dndFh; close $tmpFh;
    return ($outFile,$dndFile);
}

sub getDndData {
    my ($dndFile) = @_;
    my $dndData = "";
    open(D, "$dndFile"); #This is for the iTOL input. 
    while(<D>) {
	my $revData = reverse($_);
	$revData =~ s/:/%/;
	$revData =~ s/:/_/;
	$revData = reverse($revData);
	$revData =~ s/%/:/;
	$dndData .= "$revData\n";
    }
    close D;
    return $dndData;
}

sub getItolLink {
    my ($dndData) = @_;    
    my $ua = LWP::UserAgent->new;
    my $request = HTTP::Request::Common::POST( 'https://itol.embl.de/upload.cgi',
                                               Content_Type => 'form-data',
					       Content      => [ ttext => $dndData, ]
	);
    my $response = $ua->request($request);
    my $itolLink = "https://itol.embl.de/" . $response->{'_headers'}->{'location'};
    return $itolLink;
}

sub error {
  my ($msg) = @_;
  print "ERROR: $msg\n\n";
  exit(1);
}

sub createHTML {
  my ($itolLink,$outFile,$cgi,$userOutFormat,$dndFile) = @_;

  my $itolHtml = "<a href='$itolLink' target='_blank'><h4>Click here to view a phylogenetic tree of the alignment.</h4></a>";

  print $cgi->header('text/html');
  print "<pre>";
  &printOutput($cgi,$outFile,$userOutFormat,$itolHtml);

  print "<hr>.dnd file\n\n";
  &printDnd($dndFile);

  print "</pre>";
}

sub printOutput {
    my ($cgi,$outFile,$userOutFormat,$itolHtml) = @_;

    open(O, "$outFile") or die "can't open $outFile for reading:$!";
    while(<O>) {
	if(/CLUSTAL O/ && $userOutFormat eq "clu") {
	    print $cgi->h3($_);
	    print $cgi->pre($itolHtml);
	} else {
	    print;
	}
    }
    close O;
}

sub printDnd {
    my ($dndFile) = @_;
    open(D, "$dndFile") or die "can't open $dndFile for reading:$!"; # Printing the dendrogram on the results page.
    while(<D>) {
	print $_;
    }
    close D;
}

sub proteinTest {
    my ($dbh,$ids) = @_;
    
    my $areProteins = 0;
    my $sql = "SELECT gene_type FROM apidbTuning.TranscriptAttributes
               WHERE gene_source_id in ($ids)";
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    while(my ($geneType) = $sth->fetchrow_array()) {
	$areProteins = 1 if ($geneType eq "protein coding" || $geneType eq "protein coding gene");
    }
    $sth->finish();
    return $areProteins;
}

sub getHtsSnpSql {
    my ($ids,$sid,$start,$end) = @_;
      $ids =~ s/'(\w)/'$sid\.$1/g;
      $ids .= ",'$sid'";   # always compare with reference isolate
    my $sql = <<EOSQL;
SELECT source_id, source_id,
       substr(nas.sequence, $start,$end-$start+1) as sequence
FROM   dots.nasequence nas
WHERE  nas.source_id in ($ids)
EOSQL
    return $sql;
}

sub getOrthoProteinSql {
    my ($ids) = @_;
    my $sql = <<EOSQL;
select ps.source_id, ps.source_id, ps.sequence
from apidbtuning.proteinsequence ps, apidbtuning.transcriptattributes ta
where ta.protein_source_id = ps.source_id
      and ta.project_id = ps.project_id
      and ta.gene_source_id in ($ids)
EOSQL
    return $sql;
}

sub getOrthoCdsSql {
    my ($ids) = @_;
    my $sql = <<EOSQL;
	  with geneDetails as (
	      select ta.gene_start_min as min
	      , ta.gene_end_max as max
	      , ta.coding_start
	      , ta.coding_end
	      , ta.strand
	      , ta.source_id
	      , ta.gene_source_id
	      , ta.cds_length
	      from APIDBTUNING.transcriptattributes ta
	      where ta.gene_source_id in ($ids)
	      )
	      select ts.source_id, gd.strand,
	      CASE
	      WHEN gd.strand = 'forward' then SUBSTR(ts.sequence, (gd.coding_start - gd.min+1) , gd.cds_length)
	      WHEN gd.strand = 'reverse' then SUBSTR(ts.sequence, (gd.max - gd.coding_end+1) , (gd.cds_length))
	      END
	      from apidbtuning.transcriptsequence ts
	      , geneDetails gd
	      where gd.source_id = ts.source_id
EOSQL
    return $sql;
}

sub getOrthoGenomicIsProteinSql {
    my ($ids,$upstreamOffset,$downstreamOffset) = @_;
    my $sql = <<EOSQL;
    with geneDetails as (
	select ta.coding_start
	, ta.coding_end
	, ta.strand
	, ta.source_id
	, ta.gene_source_id
	, ta.cds_length
	, ta.sequence_id as chsmid
	from APIDBTUNING.transcriptattributes ta
	where ta.gene_source_id in ($ids)
	)
	SELECT gd.gene_source_id, gd.strand,
	CASE WHEN gd.strand = 'forward' THEN substr(gs.sequence, gd.coding_start - $upstreamOffset, (gd.coding_end - gd.coding_start) +1 + $upstreamOffset + $downstreamOffset) 
	WHEN gd.strand = 'reverse' THEN substr(gs.sequence, gd.coding_start - $downstreamOffset, (gd.coding_end - gd.coding_start ) +1 + $upstreamOffset + $downstreamOffset)
	END
	FROM ApidbTuning.GenomicSequenceSequence gs,
	geneDetails gd
	where gs.source_id = gd.chsmid
EOSQL
    return $sql;
}

sub getOrthoGenomicNotProteinSql {
    my ($ids,$upstreamOffset,$downstreamOffset) = @_;
    my $sql = <<EOSQL;
    with geneDetails as (
	select ta.gene_start_min
	, ta.gene_end_max
	, ta.strand
	, ta.source_id
	, ta.gene_source_id
	, ta.sequence_id as chsmid
	from APIDBTUNING.transcriptattributes ta
	where ta.gene_source_id in ($ids)
	)
	SELECT gd.gene_source_id, gd.strand,
	CASE WHEN gd.strand = 'forward' THEN substr(gs.sequence, gd.gene_start_min - $upstreamOffset, gd.gene_end_max - gd.gene_start_min + 1 + $upstreamOffset + $downstreamOffset)
	WHEN gd.strand = 'reverse' THEN substr(gs.sequence, gd.gene_start_min - $downstreamOffset, gd.gene_end_max - gd.gene_start_min + 1 + $upstreamOffset + $downstreamOffset)
	END
	FROM ApidbTuning.GenomicSequenceSequence gs,
	geneDetails gd
	WHERE gs.source_id = gd.chsmid
EOSQL
    return $sql;
}

sub getPopsetSql {
    my ($ids) = @_;
    my $sql = <<EOSQL;
SELECT etn.source_id, etn.source_id, etn.sequence
FROM   ApidbTuning.PopsetSequence etn
WHERE etn.source_id in ($ids)
EOSQL
    return $sql;
}

1;
