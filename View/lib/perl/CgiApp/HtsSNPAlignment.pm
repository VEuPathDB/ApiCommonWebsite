package ApiCommonWebsite::View::CgiApp::HtsSNPAlignment;

@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use EbrcWebsiteCommon::View::CgiApp;

use Bio::SeqIO;
use Bio::Seq;

use JSON;
use File::Temp qw/ tempfile /;

use Data::Dumper;

sub run {
    my ($self, $cgi) = @_;

    my $dbh = $self->getQueryHandle($cgi);
    my $type  = $cgi->param('type');

    if ($type eq 'fasta'){
	print $cgi->header('text/plain');
    } else {
	print $cgi->header('text/html');
    }

    $self->processParams($cgi, $dbh);
    $self->handleIsolates($dbh, $cgi, $type);

    exit();
}

sub processParams {
    my ($self, $cgi, $dbh) = @_;

    my $fpv = $cgi->param('filter_param_value');

    my $fpvArray = decode_json $fpv;

    my @predicates;
    foreach my $filter (@{$fpvArray->{filters}}) {
	my $filterType = $filter->{type};
	my $isRange = $filter->{isRange};
	my $field = $filter->{field};
	my $values = $filter->{value};
	next unless $values;

	if($filterType ne 'string' && $isRange) {
	    my $min = $values->{min};
	    my $max = $values->{max};
	    push @predicates, "(property = '$field' and ${filterType}_value >= $min and ${filterType}_value <= $max)";
	}
	else {
	    my $valuesString = join(',', map { "'$_'" } @$values);
	    push @predicates, "(property = '$field' and string_value in ($valuesString))";
	}
    }
    $self->{filter_predicates} = \@predicates;
}


sub handleIsolates {
    my ($self, $dbh, $cgi, $type) = @_;

    my ($start,$end,$sid,$project_id,$organism,$printMetadata,$userOutFormat) = getParams($cgi);

    (my $locationText,$end) = getLocation($sid,$start,$end);

    my $metadata = $self->getMetadata($organism,$dbh);

    my ($tab,$newline) = getTabLineChars($type);

    displayMetadata($metadata,$tab,$newline) if ($printMetadata);

    my $ids = getAllIds($metadata,$sid);

    my $sequences = getSequences($sid,$ids,$start,$end,$dbh);

    displayFasta($sequences) if ($type eq 'fasta');

    createAndDisplayAlignment($cgi,$sequences,$newline,$userOutFormat,$locationText) if ($type =~ /htsSNP/i);
}

sub getLocation {
    my ($sid,$start,$end) = @_;
    my $extraText = "";
    if (($end-$start) > 9999) {
	$end = $start + 9999;
	$extraText = "The first 10,000 nucleotides were used for alignment.";
    }
    my $locationText = " $extraText Genomic location: $sid $start to $end";
    return ($locationText,$end);
}

sub printOutput {
    my ($cgi,$outfile,$userOutFormat,$itolHtml,$locationText) = @_;

    open(O, "$outfile") or die "can't open $outfile for reading:$!";
    while(<O>) {
	if(/CLUSTAL O/ && $userOutFormat eq "clu") {
	    print $cgi->h3($_);
	    print $cgi->pre($locationText);
	    print $cgi->pre($itolHtml);
	} else {
	    print;
	}
    }
    close O;
}

sub printDnd {
    my ($dndfile) = @_;
    open(D, "$dndfile") or die "can't open $dndfile for reading:$!"; # Printing the dendrogram on the results page.
    while(<D>) {
	print $_;
    }
    close D;
}

sub getMetadata {
    my ($self,$organism,$dbh) = @_;
    my $sql = $self->getMetadataSql($organism);
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    my %metadata;
    while(my ($panId, $node, $term, $value) = $sth->fetchrow_array()) {
	$metadata{$node}->{$term} = $value;
    }
    return \%metadata;
}

sub getMetadataSql {
    my ($self,$organism) = @_;

    my $filterPredicates = $self->{filter_predicates};
    my $filterPredicatesCount = scalar @$filterPredicates;
    my $filterPredicatesString = $filterPredicatesCount ? "AND (" . join(" OR ", @$filterPredicates) . ")" : "";
    my $filterPredicatesCountString = $filterPredicatesCount ? "and ct.n = $filterPredicatesCount" : "";

    my $sql = "WITH m AS (SELECT pan_id, REPLACE(pan_name,' (Sequence Variation)','') pan_name, property, string_value
                          FROM apidbtuning.metadata
	                  WHERE dataset_subtype = 'HTS_SNP' AND organism = '$organism'
	                  $filterPredicatesString)
               SELECT m.*
               FROM m, (SELECT pan_id, COUNT(*) n FROM m GROUP BY pan_id) ct
               WHERE ct.pan_id = m.pan_id 
               $filterPredicatesCountString";

    return $sql;
}

sub getParams {
    my ($cgi) = @_;
    my $start = $cgi->param('start');
    my $end   = $cgi->param('end');
    my $sid   = $cgi->param('sid');
    my $project_id = $cgi->param('project_id');
    my $organism   = $cgi->param('organism');
    my $printMetadata  = $cgi->param('metadata');
    my $userOutFormat = $cgi->param('clustalOutFormat');
    if ((! defined $userOutFormat) || ($userOutFormat eq "")){
	$userOutFormat = "clu";
    }
    $start =~ s/,//g;
    $end =~ s/,//g;
    return ($start,$end,$sid,$project_id,$organism,$printMetadata,$userOutFormat);
}

sub getTabLineChars {
    my ($type) = @_;
    my ($tab,$newline) = ("&nbsp;&nbsp;","<BR>");
    if ($type eq 'fasta'){
	($tab,$newline) = ("\t","\n");
    }
    return ($tab,$newline);
}

sub displayMetadata {
    my ($metadata,$tab,$newline) = @_;
    print "### Metadata for the Strains: $newline";
    foreach my $key (sort keys %{$metadata}) {
	print "#Isolate=$key :  ";
	foreach my $ca (sort keys %{$metadata->{$key}}) {
	    print "$ca= ". $metadata->{$key}->{$ca} . "$tab" ;
	}
	print "$newline";
    }
    print "$newline$newline";
}

sub getAllIds {
    my ($metadata,$sid) = @_;
    my $ids = join(",", map { "'$_'" } keys %{$metadata});
    $ids =~ s/'(\w)/'$sid\.$1/g;
    $ids .= ",'$sid'";   # always compare with reference isolate
}

sub getSequences {
    my ($sid,$ids,$start,$end,$dbh) = @_;
    my $sql = getSequencesSql($ids,$start,$end,$dbh);
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    my %sequences;
    while(my ($id, $seq) = $sth->fetchrow_array()) {
	my $noN = $seq;
	$noN =~ s/[ACGT]//g;
	next if length($noN) == length($seq);
	$id =~ s/^$sid\.// unless ($id eq $sid);
	$sequences{$id}= $seq;
    }
    return \%sequences;
}

sub getSequencesSql {
    my ($ids,$start,$end,$dbh) = @_;
    my $sql = "SELECT source_id, SUBSTR(nas.sequence, $start,$end-$start+1) AS sequence
               FROM dots.nasequence nas
               WHERE nas.source_id IN ($ids)";
    return $sql;
}

sub displayFasta {
    my ($sequences) = @_;
    my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');
    foreach my $id (keys %{$sequences}){
	my $sequence = $sequences->{$id};
	$sequence =~ s/-//g;
	my $fastaSeq = Bio::Seq->new( -seq => $sequence,
                                      -display_id  => $id
                                    );
	$seqIO->write_seq($fastaSeq);
    }
    $seqIO->close();
}

sub createAndDisplayAlignment {
    my ($cgi,$sequences,$newline,$userOutFormat,$locationText) = @_;
    my ($infh, $infile)  = tempfile();
    my ($outfh, $outfile) = tempfile();
    my ($dndfh, $dndfile) = tempfile();
    my ($tmpfh, $tmpfile) = tempfile();

    writeInFile($sequences,$infh);

    close $infh; close $dndfh; close $outfh; close $tmpfh;

    my $cmd = "clustalo -v --residuenumber --infile=$infile --outfile=$outfile --outfmt=$userOutFormat --output-order=tree-order --guidetree-out=$dndfile --force > $tmpfile";
    my $exitCode = system($cmd);
    clustalError($exitCode,$tmpfile,$newline) if ($exitCode != 0);
    
    my $dndData = getDndData($dndfile);
    my $itolLink = getItolLink($dndData);
    createHtml($cgi,$itolLink,$outfile,$dndfile,$userOutFormat,$locationText);
}

sub writeInFile {
    my ($sequences,$infh) = @_;
    foreach my $id (keys %{$sequences}){
	my $sequence = $sequences->{$id};
	print $infh ">$id\n$sequence\n";
    }
}

sub clustalError {
    my ($exitCode,$tmpfile,$newline) = @_;
    print "ERROR. Problem running clustalo. Exit code: $exitCode. Clustalo log: $newline";
    open(IN,$tmpfile);
    while (<IN>) {
	print "$_ $newline";
    }
    close IN;
    print "$newline$newline";
    die;
}

sub getItolLink {
    my ($dndData) = @_;
    my $ua = LWP::UserAgent->new;
    my $request = HTTP::Request::Common::POST( 'https://itol.embl.de/upload.cgi',
					       Content_Type => 'form-data',
					       Content      => [ ttext => $dndData, ]
	                                     );
    my $response = $ua->request($request);
    my $itolLink =  "https://itol.embl.de/" . $response->{'_headers'}->{'location'};
    return $itolLink;
}

sub getDndData {
    my ($dndfile) = @_;
    my $dndData = "";
    open(D, "$dndfile"); #This is for the iTOL input. 
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

sub createHtml {
    my ($cgi,$itolLink,$outfile,$dndfile,$userOutFormat,$locationText) = @_;

    my $itolHtml = "<a href='$itolLink' target='_blank'><h4>Click here to view a phylogenetic tree of the alignment.</h4></a>";

    print $cgi->header('text/html');
    print "<pre>";
    &printOutput($cgi,$outfile,$userOutFormat,$itolHtml,$locationText);

    print "<hr>.dnd file\n\n";
    &printDnd($dndfile);
  
    print "</pre>";
}

1;
