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

  my $dbh = $self->getQueryHandle($cgi);

  print $cgi->header('text/html');

  $self->processParams($cgi, $dbh);
  $self->handleIsolates($dbh, $cgi);

  exit();
}

sub processParams {
  my ($self, $cgi, $dbh) = @_;
  my $type  = $cgi->param('type');

  # print DUMPER $type;
  if($type eq 'geneOrthologs') {
    my @ids = $cgi->param('gene_ids');
    $self->{ids} = join(',', @ids);
  }

  # JB: not sure why this is splitting then concatenating.  Seems like all this is doing is stripping the trailing comma
  else {
    my $p = $cgi->param('isolate_ids');
    $p =~ s/,$//;
    my @ids = split /,/, $p;
    my $list;
    foreach my $id (@ids){
      $list = $list.  "'" . $id. "',";
    }
    $list =~ s/\,$//;
    $self->{ids} = $list;
  }
}

sub handleIsolates {
  my ($self, $dbh, $cgi) = @_;

  my $GUS_HOME = $ENV{'GUS_HOME'};

  my $ids = $self->{ids};

  my $type  = $cgi->param('type');
  my $start = $cgi->param('start');
  my $end   = $cgi->param('end');
  my $sid   = $cgi->param('sid');
  my $project_id = $cgi->param('project_id');

  # Used to determine the SQL query for the gene page clustalo.
  my $clustalQueryType = $cgi->param('sequence_Type');
  if ((! defined $clustalQueryType) || ($clustalQueryType eq "")){
      $clustalQueryType = "protein";
  }

  $start =~ s/,//g;
  $end =~ s/,//g;

  if($end !~  /\d/) {
      $end   = $start + 50;
      $start = $start - 50;
  }
  my $sql = "";  #NOTE - the sql needs to return an array of 3 values. Only the gene page Clustal Omega uses 3 values, beware the order of values [id, strand, seq]. Strand is only used for the genomic query, so a dummy value is used elsewhere.

  if($type =~ /htsSNP/i) {
      $ids =~ s/'(\w)/'$sid\.$1/g;
      $ids .= ",'$sid'";   # always compare with reference isolate
      $sql = <<EOSQL;
SELECT source_id,
	   source_id
       substr(nas.sequence, $start,$end-$start+1) as sequence,
FROM   dots.nasequence nas
WHERE  nas.source_id in ($ids)
EOSQL
   } elsif($type eq 'geneOrthologs') {
      $ids = join(',', map { "'$_'" } split(',', $ids));

      if($clustalQueryType eq 'protein' ){
	  $sql = <<EOSQL;
	  select ps.source_id, ps.source_id,  ps.sequence
	      from apidbtuning.proteinsequence ps, apidbtuning.transcriptattributes ta
	      where ta.protein_source_id = ps.source_id
	      and ta.project_id = ps.project_id
	      and ta.gene_source_id in ($ids)
EOSQL
      } elsif($clustalQueryType eq 'CDS'){
	  $sql = <<EOSQL;
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
      } elsif($clustalQueryType eq 'genomic'){

	      my $areProteins = proteinTest($dbh,$ids);

	      my $upstreamOffset = $cgi->param('oneOffset');
	      $upstreamOffset = abs($upstreamOffset);
	      my $downstreamOffset = $cgi->param('twoOffset');
	      $downstreamOffset = abs($downstreamOffset);

	      # Alters user input if user selects >2500 nt.
	      if ($upstreamOffset > 2500) {$upstreamOffset = 2500;}
	      if ($downstreamOffset > 2500) {$downstreamOffset = 2500;}

	      if ($areProteins) {
		  $sql = <<EOSQL;
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
	      } else {            # not protein encoding
                  $sql = <<EOSQL;
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
	     }
        } else {  # regular isolates
	    $sql = <<EOSQL;
SELECT etn.source_id, etn.source_id, etn.sequence
FROM   ApidbTuning.PopsetSequence etn
WHERE etn.source_id in ($ids)
EOSQL
        }
  }
  my $sequence;
  my $sth = $dbh->prepare($sql);

  $sth->execute();
  while(my ($id, $strand, $seq) = $sth->fetchrow_array()) {
    # print STDERR $seq;
  	if ($strand eq 'reverse' && $clustalQueryType eq "genomic"){
  	    my $seqR = Bio::Seq->new(-seq => $seq, alphabet => 'dna');
  	    $seqR = $seqR->revcom();
  		$seq = $seqR->seq;
  	}
  	elsif($strand eq 'forward' && $clustalQueryType eq "genomic"){;}
    else{;} # For protein/CDS there is no need to check the strand, so this should pass the seq through for whatever is the other value returned by the SQL query.

    $id =~ s/^$sid\.// unless ($id eq $sid);
    my $noN = $seq;
    $noN =~ s/[ACGT]//g;
    next if length($noN) == length($seq);
    $sequence .= ">$id\n$seq\n";
  }

  my ($infh, $infile)  = tempfile();
  my ($outfh, $outfile) = tempfile();
  my ($dndfh, $dndfile) = tempfile();
  my ($tmpfh, $tmpfile) = tempfile();

  print $infh $sequence;
  close $infh;

  my $userOutFormat = $cgi->param('clustalOutFormat');
  if ((! defined $userOutFormat) || ($userOutFormat eq "")){
	     $userOutFormat = "clu";
  }

  my $cmd = "clustalo -v --residuenumber --infile=$infile --outfile=$outfile --outfmt=$userOutFormat --output-order=tree-order --guidetree-out=$dndfile --force > $tmpfile";
  system($cmd);
  my %origins = ();
  my $dndData = "";

  open(D, "$dndfile"); #This is for the iTOL input. 
  while(<D>) {
	#  print $_;
    my $revData = reverse($_);
    $revData =~ s/:/%/;
    $revData =~ s/:/_/;
    $revData = reverse($revData);
    $revData =~ s/%/:/;
    $dndData = $dndData . $revData . "\n";
  }
  close D;

  ## Interacting with iTOL to make a tree.
  ## NOTE - check elsewhere this is used when done. SNP etc.
  ## This uses the dnd file out put.

  my $ua = LWP::UserAgent->new;
  my $request = HTTP::Request::Common::POST( 'https://itol.embl.de/upload.cgi',
     Content_Type => 'form-data',
     Content      => [
                       ttext => $dndData,
                     ]);
  my $response = $ua->request($request);
  # print Dumper $response->{'_headers'}->{'location'};
  # print Dumper $response->content;
  my $iTOLLink =  "https://itol.embl.de/" . $response->{'_headers'}->{'location'};
  my $iTOLHTML = "<a href='$iTOLLink' target='_blank'><h4>Click here to view a phylogenetic tree of the alignment.</h4></a>";
  &createHTML($iTOLHTML,$outfile,$cgi,%origins);

  open(D, "$dndfile"); # Printing the dendrogram on the results page.
  print "<pre>";
  print "<hr>.dnd file\n\n";
  while(<D>) {
	print $_;
  }
}

sub error {
  my ($msg) = @_;
  print "ERROR: $msg\n\n";
  exit(1);
}


sub createHTML {
  my ($iTOLLINK, $outfile, $cgi, %origins) = @_;
  open(O, "$outfile") or die "can't open $outfile for reading:$!";

  my $userOutFormat = $cgi->param('clustalOutFormat');
  if ((! defined $userOutFormat) || ($userOutFormat eq "")){
    $userOutFormat = "clu";
  }

  print "<pre>";
    while(<O>) {
      if(/CLUSTAL O/ && $userOutFormat eq "clu") {
        print $cgi->h3($_);
        print $cgi->pre($iTOLLINK);
      }
      else {
        print;
      }
    }
   close O;
  print "</pre>";
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

1;
