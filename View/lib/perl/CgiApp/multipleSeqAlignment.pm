package ApiCommonWebsite::View::CgiApp::multipleSeqAlignment;
use base qw( ApiCommonWebsite::View::CgiApp );

use strict;
use File::Basename;
use File::Temp qw(tempfile);
use IO::String;
use Bio::SeqIO;
use Bio::Seq;
use Bio::SimpleAlign;
use Bio::AlignIO;
use Data::Dumper;
use Bio::Location::Simple;
use Bio::Coordinate::Pair;
use Bio::Coordinate::GeneMapper;
use WDK::Model::ModelProp;
use EuPathSiteCommon::Model::ModelXML;
use CGI;
use CGI::Carp qw(fatalsToBrowser set_message);
use ApiCommonWebsite::View::CgiApp::IsolateClustalw;
#use ApiCommonWebsite::View::CgiApp::htmlClustalo;
use Bio::Graphics::Browser2::PadAlignment;

sub run {
    my ($self, $cgi) = @_;
    my %alignmentHash;
    my %originsHash;
#    my @alignmentsPad;
    my $project = $cgi->param('project_id');
    my $contig = $cgi->param('contig');
    my $start= $cgi->param('start');
    my $stop= $cgi->param('stop');
    my $revComp = $cgi->param('revComp');
    my $genomes = $cgi->param ('genomes');
    my $type= $cgi->param('type');
    if ($type eq 'clustal') {
	print $cgi->header('text/html');
    }
    elsif ($type eq 'fasta_ungapped'){
	print $cgi->header('text/plain');
    }
    elsif ($type eq 'fasta_gapped'){
	print $cgi->header('text/plain');
    }
    my $dbh = $self->getQueryHandle($cgi);
    my $taxonToDirNameMap = getTaxonToDirMap($cgi, $dbh);
    
    my ($contig, $start, $stop, $strand, $type, $referenceGenome, $genomes) = &validateParams($cgi, $dbh, $taxonToDirNameMap);
    my ($mercatorOutputDir, $pairwiseDirectories, $availableGenomes) = &validateMacros($cgi);

    my $regex = join '|', @$genomes;

    unless($regex) {
      print "Please choose at least one organism which is not the reference";
    }

    foreach my $pairs (@$pairwiseDirectories) {

	my %agpHash;
	my @agpArraybackwards;
	my @fullCoordCheck;
	my $pair = basename($pairs);

	if ($regex && ($pair =~ /$referenceGenome/) && ($pair=~ /$regex/)) {
#	    my @org_names = split "-", $pair;
            my @org_names = map { s/\.agp//; basename($_); } glob($pairs . "/*.agp");

#	    print Dumper "pair is $pair";
	    foreach my $elements (@org_names) {
		
		my ($agpHashRef, $backwardAgpArrayRef, $coordRef) = &makeAgpMap($pairs, $pair, $elements);
		my %agp = %{$agpHashRef};
		my @backwardAgp = @{$backwardAgpArrayRef};
		my @coord = @{$coordRef};

		foreach my $agpkey (keys %agp) {
		    if (exists $agpHash{$agpkey}) {
			print Dumper "ERROR YOU HAVE THE AGP MAP THE WRONG WAY ROUNG\n";
		    }
		    else {
			$agpHash{$agpkey}=$agp{$agpkey};
		    }
		}
		foreach my $agpBack (@backwardAgp) {
		    my %agpbackHash = %{$agpBack};
		    push (@agpArraybackwards, \%agpbackHash)
		}
		foreach my $co (@coord) {
		    my %coordHash = %{$co};
		    push (@fullCoordCheck, \%coordHash)
		}
	    }
#removed @alignments from front array below 
	    my ($sequenceHashRef,$orHashRef) = &createAlignmentHash($pairs, \%agpHash, $referenceGenome, $start, $stop, $contig, $strand, \@agpArraybackwards, $revComp, \@fullCoordCheck);

	    my %sequenceHash = %$sequenceHashRef;
     
	    my %orHash= %$orHashRef;
	    foreach my $sequenceToAlign (keys %sequenceHash){
		if (exists $alignmentHash{$sequenceToAlign}) {
		    print Dumper "ERROR $sequenceToAlign already in alignment hash\n";
		}
		else {
		    $alignmentHash{$sequenceToAlign} = $sequenceHash{$sequenceToAlign};
#		    print Dumper "seq to align is $sequenceToAlign";
		}
	    }
	    foreach my $startPoint (keys %orHash) {
		if (exists $originsHash{$startPoint}) {
		    print Dumper "ERROR $startPoint already in origins hash\n";
		}
		else {
		    $originsHash{$startPoint} = $orHash{$startPoint};
		}
	    }
	  #  my @alignmentsArray = @alignments;
	   # foreach my $arrayRef (@alignmentsArray) {
		
	#	push  (@alignmentsPad , $arrayRef);
	 #   }
	    
	}
    }
    my $referenceSequence = &getReferenceSequence($project, $contig, $referenceGenome, $start, $stop, $dbh);
    my $referenceId = $contig;
    if ($revComp eq 'on') {
	$referenceSequence = &reverseCompliment($referenceSequence);
	$alignmentHash{$referenceId}=$referenceSequence;
	my $negstart = (-1 * $start);
	$originsHash{$referenceId}=$negstart;
   }
    else {
    $alignmentHash{$referenceId}=$referenceSequence;
    $originsHash{$referenceId}=$start;
    }
#    print Dumper "alignments";
 #   print Dumper @alignmentsPad;
 #   print Dumper %originsHash;
#    print Dumper "HASH";
 #   print Dumper %alignmentHash;
    my $tempfile = &doClustalWalignment(\%alignmentHash, $mercatorOutputDir, $referenceId);
    if ($type eq 'clustal') {
	ApiCommonWebsite::View::CgiApp::IsolateClustalw::createHTML($tempfile,$cgi,%originsHash);
#	my $in  = Bio::AlignIO->new(-file   => $tempfile,
#				    -format =>'clustalo');
#	my $out = Bio::AlignIO->new(-fh   => \*STDOUT,
 #                           -format => 'clustalo');
#
 #  while ( my $aln = $in->next_aln() ) {
 #       $out->write_aln($aln);
 #   } 
   }
    elsif($type eq 'fasta_ungapped') {
	my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');
	
	my $seenReference;
	foreach my $seq (keys %alignmentHash) {
	    my $id = $seq;
	    next if($seenReference and $id eq $contig);
	    if($id eq $contig) {
		$seenReference++;
	    }
	    
	    my $sequence = $alignmentHash{$seq};
	    $sequence =~ s/-//g;
	    
	    my $ungappedSeq = Bio::Seq->new( -seq => $sequence,
					     -id  => $id
		);
	    
	    $seqIO->write_seq($ungappedSeq);
	}
	$seqIO->close();
    }
    elsif($type eq 'fasta_gapped') {
	my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');
	
	my $seenReference;
	foreach my $seq (keys %alignmentHash) {
	    my $id = $seq;
	    next if($seenReference and $id eq $contig);
	    if($id eq $contig) {
		$seenReference++;
	    }
	    
	    my $sequence = $alignmentHash{$seq};
	    
	    my $gappedSeq = Bio::Seq->new( -seq => $sequence,
					     -id  => $id
		);
	    
	    $seqIO->write_seq($gappedSeq);
	}
	$seqIO->close();
    }
    
    
    
    
    
}


sub createAlignmentHash {

    my ($folder, $agpHashRef, $ref, $start, $stop, $contig, $strand, $backArrayRef, $revComp, $coordRef) = @_;
    my $mapfile = $folder."/alignments/map";
    my @pairNames = split "-", basename($folder);
    my $first = $pairNames[0];
    my $second = $pairNames[1];
    my $other;
    if ($first eq $ref) {
	$other = $second;
    }
    elsif ($second eq $ref) {
	$other = $first;
    }
    else {
	print Dumper "cant determine comparator\n ";
    }
    
    my %sequenceHash;
    my %originsHash;
    my @alignments;
    my $regionFound=0;
    open (IN, "$mapfile") or die "can't open the map file for $folder to determine alignment folders\n";
#    print Dumper "map file $mapfile";
    while (my $line = <IN>) {
	chomp $line;
	
        my ($alignmentFolder, $refSourceId, $refStart, $refEnd, $refStrand, $otherSourceId, $otherStart, $otherEnd, $otherStrand);
	
	my @temps = split "\t", $line;
	my $agp=$agpHashRef->{$contig};
	
	my $userCoordObj = Bio::Location::Simple->new( -seq_id => $contig,
						       -start => $start,
						       -end =>  $stop,
						       -strand => $strand );  #make sure strand I pass is bioperl strand - this should of been dome when i checked parameters  
	
	my $result = $agp->map($userCoordObj);
#	print Dumper $result;
	my $detStartFromUser = $result->start; 
	my $detEndFromUser =$result->end;
	
	
	my $refContig = $result->seq_id;
	my $sliceStart;
	my $sliceEnd;
	if ($temps[1] =~/$refContig/) {
	    ($alignmentFolder, $refSourceId, $refStart, $refEnd, $refStrand, $otherSourceId, $otherStart, $otherEnd, $otherStrand) = @temps;
	}
	elsif($temps[5] =~/$refContig/) {
	    ($alignmentFolder,$otherSourceId,$otherStart,$otherEnd,$otherStrand,$refSourceId,$refStart, $refEnd, $refStrand) = @temps;
	}
	else {
	    next; 
	}
	my ($sliceStart, $sliceEnd, $useThisRegion) =&determineSlice($detStartFromUser, $detEndFromUser, $refStart, $refEnd, $alignmentFolder, $other);
	if ($useThisRegion == 1) {
	    $regionFound =1;
	    my $folderToGetMfa = $folder."/alignments/".$alignmentFolder;
#	    print Dumper "$folderToGetMfa $sliceStart $sliceEnd";
	    my $alignmentObj;
	    opendir(my $dh, $folderToGetMfa) || die "Can't open $folderToGetMfa: $!";
	    while (readdir $dh) {
		
		my $file = $folderToGetMfa."/".$_;
		
		if ($file =~ /mavid.mfa.gz$/) {
		    
		    open (my $z, '-|', '/usr/bin/gunzip', '-c', $file) or die "can't open $file $!";
		    $alignmentObj = Bio::AlignIO->new(-fh => \*$z, -format => 'fasta');    
		    
		}
		elsif($file=~ /mavid.mfa$/) {
		    
		    
		    
		    open (my $z, "$file") or die "cant open $folderToGetMfa$!";
		    $alignmentObj = Bio::AlignIO->new(-fh => \*$z, -format => 'fasta');    
		    
		}
		else { 
		    next;
		}
	    }
	    
	    closedir $dh;
	    
	    my $aln = $alignmentObj->next_aln();
	    my $length = $aln->length();
	    my $start_pos = $aln->column_from_residue_number( $ref, $sliceStart);
	    my $end_pos = $aln->column_from_residue_number( $ref, $sliceEnd);
#          print Dumper "start $start_pos end $end_pos other $other";
	    my $aln2 = $aln->slice($start_pos,$end_pos);
	    my $aln3 = $aln2->remove_gaps;
#		print Dumper "alingment3\n";
#		print Dumper $aln3;
#		print Dumper "other is \n";
#		print Dumper $other;
	    my $seq = $aln3->get_seq_by_id($other);   
#	    print Dumper $other;
#	    print Dumper $seq;
	    if (defined $seq) {
#		my $sublength = $end_pos-$start_pos;
		my $seqonly = $seq->seq;
#		my $seq = $aln->seq;
#		my $substring = substr($seq, $start_pos, $sublength);
		my $id = $seq->id;
#		print Dumper "seq id is $id";
		my $seqStart = $seq->start;
		my $seqEnd = $seq->end;
#		print Dumper "start end $seqStart $seqEnd other $otherStart $otherEnd";
		my @agpback = @{$backArrayRef};
		my @coordCheck = @{$coordRef};
		my $OtherContigId;
#		my $strandCheck;
#		if ($otherStrand eq '+') {
#		     $strandCheck = 1;
#		}
#		else {
#		     $strandCheck = -1;
#		}
		my %matches;
		my $matchCount =0;
		foreach my $element (@agpback) {
		    my $agp2= $element->{$otherSourceId};
		    my $startToPull;
		    my $endToPull;
		    if (defined $agp2) {
#			print Dumper "agp2\n";
#			print Dumper $agp2;
			if ($agp2->in->seq_id eq $otherSourceId) { 
#			    my %matches;
#			    print Dumper "agp choosen";
#			    print Dumper $agp2;
			    #my $strandTest = $agp2->in->strand;
			    # if ($strandTest eq $strandCheck) {
			    my $start = $agp2->in->start;
			    my $end = $agp2->in->end;
			    my $startContig = $agp2->out->start;
			    my $endContig = $agp2->out->end;
#				print Dumper $start;
			    if ($otherStrand eq '-') {
#				print Dumper "negative strand" ;
#				print Dumper "$otherEnd $seqEnd"; 
				$startToPull = $otherEnd - $seqEnd;
				$endToPull= $startToPull +($seqEnd-$seqStart);
			    }
			    else {
				$startToPull = $seqStart;
				$endToPull = $seqEnd;
			    }
			    my $lala = $agp2->in->seq_id;
			    my $lalala = $agp2->out->seq_id;
#			    print Dumper "start pull $startToPull $endToPull $lala $lalala $start $end";
			    if ((($start < $startToPull) && ($end < $endToPull) && ($end > $startToPull)) || (( $start >= $startToPull) && ($start <= $endToPull)) || (($start >= $startToPull) && ($end <=$endToPull)) || (($startToPull > $start) && ($endToPull < $end))) {
				$OtherContigId = $agp2->out->seq_id;
				@{$matches{$OtherContigId}} = ($seqonly,$start,$end,$startToPull,$endToPull,$startContig,$endContig,$refStrand,$otherStrand);
#				    print Dumper "YES $OtherContigId";
				$matchCount ++;
			    }
			    else { 
				my  $OtherContigId2 = $agp2->out->seq_id;
#				print Dumper "Skipping $OtherContigId2";
				next;
			    }
			    # }
			    #   else {
#			#	print Dumper "stand is $strandTest";
			    #	next;
			    #   }
			}
			
#			if ($revComp eq 'on') {
#			    $seqonly = &reverseCompliment($seqonly);
			    #####reverse complement the sequence here
#			    if ($otherStrand eq '+') {
#				my $negseqStart = (-1 * $seqStart);
#				$originsHash{$uniqueid}=$negseqStart;
#			    }
#			    else { 
#				$originsHash{$uniqueid}=$seqStart;
#			    }
#			}
#			else {
#			    if ($otherStrand eq '-'){
#				my $negseqStart = (-1 * $seqStart);
#				$originsHash{$uniqueid}=$negseqStart;
#			    }
#			    else {
#				$originsHash{$uniqueid}=$seqStart;
#			    }
#			}
		    }
		    else {
			next;
		    }

		}
#			    print Dumper "only getting here after looking at each one";
		
		my (%hashOfNewSeqCoords) = &determineSplitSeq(%matches);
		#removed @tempAlignments from array and slice start and end from sub 
		my ($tempSeqHashRef, $tempOriHashRef) = &workOutSeq($revComp,%hashOfNewSeqCoords);
		my %tempSeqHash = %{$tempSeqHashRef};
		my %tempOriHash = %{$tempOriHashRef};
		foreach my $one (keys %tempSeqHash) {
		    $sequenceHash{$one} = $tempSeqHash{$one};
		}
		foreach my $two (keys %tempOriHash) {
		    $originsHash{$two}=$tempOriHash{$two};
		}
#		foreach my $three (@tempAlignments){
#		    push (@alignments , $three);
#		}
		    #HERE DO THE SPLITING ETC ETC 
#		    my ($finalSeq, finalOrigin) = &workOutSeq(
#			print Dumper "array is @alignArray";
#			print Dumper "overall is @alignments";
#			last;
###			}
		
###			else {
###			    next;
#		    }
	    }
	    else{
		next;
	    }
	}
	
	elsif ($useThisRegion ==0) {
	    next;
	}
	
	else {
	    print Dumper "there is an error determining if the region should be used. please check the determineSlice subroutine\n";
	}
    }
    
 #   print Dumper "region found $regionFound $other";
    #removed @alignments
 #   print Dumper "SEQUENCE HASH";
 #   print Dumper %sequenceHash; 
   # print Dumper "origins hash";
   # print Dumper %originsHash;
    return (\%sequenceHash, \%originsHash);
    
}
sub doClustalWalignment {
    my ($sequenceHash,$folder, $reference) = @_;
    my %hash = %$sequenceHash;
#    print Dumper "folder is $folder \n ";
#    print Dumper "reference is $reference";
#    my $multifasta = $folder."sequences.fasta";
    my ($fh1, $multifasta) = tempfile();
#    print Dumper "multifasta is $multifasta \n ";
    my $seqin = Bio::SeqIO->new (-file=> ">$multifasta", -format=>'fasta');
#    my $outfile = $folder."clustalw.aln";
#    my $tempfile = $folder."tempclustalstderr.txt";
    my ($fh2, $outfile) = tempfile();
    my ($fh3, $tempfile) = tempfile();
#    print Dumper "outfile is $outfile and tempfile is $tempfile \n ";
    my $newFastaObj = Bio::Seq->new (-id => $reference, -seq =>$hash{$reference});
    $seqin->write_seq($newFastaObj);
    foreach my $element (keys %hash) {
	if ($element eq $reference) {
	    next;
#	    print Dumper "skipping $element";
	}
	else{
#	print Dumper $element;
	my $newFastaObj = Bio::Seq->new (-id => $element, -seq => $hash{$element});
	$seqin->write_seq($newFastaObj);
	}
    }
    
#    my $cmd = "/usr/bin/clustalw2 -infile=$multifasta -outfile=$outfile -OUTORDER=input -SEQNOS=on > $tempfile";
#    if(-e $outfile) {
#	print Dumper "it exists\n";
#	}
   my $cmd = "/usr/bin/clustalo --infile=$multifasta --outfile=$outfile --outfmt clustal --output-order tree-order --seqtype dna  --force 2> $tempfile";
    system($cmd);
#    open (IN, $tempfile) or die "cant open $tempfile!\n";
 #   while (<IN>){
#	print Dumper $_;
 #   }
  #  close IN;
    my $cmd = " rm $multifasta $tempfile";
    system($cmd);
    return $outfile;
    
}





sub getTaxonToDirMap {
    my ($cgi,$dbh)  = @_;
    my $taxonToDirMap;
    
    my $project = $cgi->param('project_id');
    
    my $sql="
 	SELECT distinct ga.organism, taxon.grp, org.abbrev
 	    FROM   ApiDBTuning.TranscriptAttributes ga, ApiDB.Organism org,
 	    (SELECT organism, row_number() over (order by organism) as grp
 	     FROM (SELECT distinct organism FROM ApiDBTuning.TranscriptAttributes)
 	    ) taxon
 	    WHERE  ga.taxon_id = org.taxon_id
 	    AND    ga.gene_type = 'protein coding'
	    AND    ga.organism = taxon.organism";
    
    
    my $sth = $dbh->prepare($sql);
    $sth->execute();
    
    while (my $hashref = $sth->fetchrow_hashref()) {
	$taxonToDirMap->{$hashref->{ORGANISM}} = {name => $hashref->{ABBREV}, group => $hashref->{GRP} };
    }
    
    return $taxonToDirMap;
}



sub validateMacros {
    my ($cgi) = @_;
    
    my $project = $cgi->param('project_id');
       my $props =  WDK::Model::ModelProp->new($project);
       my $model = EuPathSiteCommon::Model::ModelXML->new('apiCommonModel.xml');
    
       my $buildNumber = $model->getBuildNumberByProjectId($project);
       my $wsMirror = $props->{WEBSERVICEMIRROR};
    
      my $mercatorOutputDir = $wsMirror . "/$project/build-$buildNumber/mercator_pairwise/";
 #   my $mercatorOutputDir = "/var/www/PlasmoDB/plasmodb.pulmanj/plasmo/";
 #      print Dumper "folder is $mercatorOutputDir\n";
    opendir(DIR, $mercatorOutputDir) || die "Can't open $mercatorOutputDir for reading:$!";
    my @pairwiseDirs = grep { -d "$mercatorOutputDir/$_" &&  /[a-zA-Z0-9_-]/ } readdir(DIR);
    closedir DIR;
    
    my @pairwiseDirectories;
    
    
    my %genomesHash;
    foreach my $dir (@pairwiseDirs) {
      my @genomes = map { s/\.agp//; basename($_); } glob($dir . "/*.agp");
	
#	my (@genomes) = split("-", $dir);
	if(scalar @genomes == 2) {
	    $genomesHash{$genomes[0]} = 1;
	    $genomesHash{$genomes[1]} = 1;
	}
	
	push @pairwiseDirectories, "$mercatorOutputDir/$dir";
	
	my $alignmentsDir = "$mercatorOutputDir/$dir/alignments";
	
	unless(-e $alignmentsDir) {
	    print STDERR "ALIGNMENTS dir $alignmentsDir not found\n";
	    error("alignments directory not found");
	}
    }
    
    my @availableGenomes = keys %genomesHash;
    
    return($mercatorOutputDir, \@pairwiseDirectories, \@availableGenomes);
}



sub validateParams {
    my ($cgi, $dbh, $taxonDirHash) = @_;
    
    my $contig       = $cgi->param('contig');
    my $start        = $cgi->param('start');
    my $stop         = $cgi->param('stop');
    my $revComp      = $cgi->param('revComp');
    my $type         = $cgi->param('type');
    
    my @genomes      = $cgi->param('genomes');
    if(scalar @genomes < 1) {
	&userError("You must select at least one genome to align to");
    }
    
    my $organism = &getOrganismFromContig($contig, $dbh);
    
    my $referenceGenome;
    
    unless($referenceGenome = $taxonDirHash->{$organism}->{name}) {
	&userError("Invalid Genome Name [$organism]: does not match an available Organism");
    }
    
    my $strand;
    if($revComp eq 'on') {
	$strand = '-';
    }
    else {
	$strand = '+';
    }
    
    unless($type eq 'clustal' || $type eq 'fasta_gapped' || $type eq 'fasta_ungapped') {
	&userError("Invalid Type [$type]... expected clustal,fasta_gapped,fastaungapped");
    }
    
    $start =~ s/[,.+\s]//g;
    $stop =~ s/[,.+\s]//g;
    $start = 1 if (!$start || $start !~/\S/);
    $stop = 1000000 if (!$stop || $stop !~ /\S/);
    &userError("Start '$start' must be a number") unless $start =~ /^\d+$/;
    &userError("End '$stop' must be a number") unless $stop =~ /^\d+$/;
    if ($start < 1 || $stop < 1 || $stop <= $start) {
	&userError("Start and End must be positive, and Start must be less than End");
    }
    
    my $length = $stop - $start + 1;
    if($length > 100000) {
	&userError("Values provided exceed the Maximum Allowed Alignemnt of 100KB");
    }

    my @filteredGenomes;
    foreach(@genomes) {
      push @filteredGenomes, $_ unless($_ eq $referenceGenome);
    }

    print STDERR Dumper \@filteredGenomes;
    
    return ($contig, $start, $stop, $strand, $type, $referenceGenome, \@filteredGenomes);
}


sub getOrganismFromContig {
    my ($contig, $dbh) = @_;
    
    my $sql="
    SELECT source_id, organism
	FROM ApidbTuning.GenomicSeqAttributes 
	WHERE  upper(source_id) = ?";
    
    my $sth = $dbh->prepare($sql);
    $sth->execute(uc($contig));
    
    my $organism;
    if(my @a = $sth->fetchrow_array()) {
	$organism = $a[1];
    }
    else {
	&userError("Invalid source ID:  $contig\n");
    }
    
    $sth->finish();
    
    return $organism;
}


sub determineSlice {
    my ($detStartFromUser, $detEndFromUser,$refStart,$refEnd,$alignmentFolder, $other) = @_;
    my $sliceStart;
    my $sliceEnd;
    my $useThisRegion;
    if (($detStartFromUser <= $refStart) && ($detEndFromUser >= $refEnd)) {
#	$sliceStart = $detStartFromUser;
#	$sliceEnd = $detEndFromUser;
	$sliceStart = 1;
	$sliceEnd = $refEnd-$refStart;
	$useThisRegion = 1;
#	print Dumper "1st $other aln is $alignmentFolder det start $detStartFromUser det end $detEndFromUser start $refStart end $refEnd";
    }
    elsif (($detStartFromUser > $refStart) && ($detEndFromUser < $refEnd)) {
#	$sliceStart = $refStart;
#	$sliceEnd =$refEnd;
	$sliceStart = $detStartFromUser-$refStart;
	$sliceEnd = ($sliceStart+($detEndFromUser-$detStartFromUser));
	$useThisRegion =1;
#	print Dumper "2nd $other aln is $alignmentFolder det start $detStartFromUser det end $detEndFromUser start $refStart end $refEnd";
    }
    elsif (($detStartFromUser > $refStart) && ($detEndFromUser <$refEnd) && ($detStartFromUser < $refEnd)) {
#	$sliceStart = $refStart;
#	$sliceEnd =$detEndFromUser;
	$sliceStart = $detStartFromUser-$refStart;
	$sliceEnd = ($refEnd - $refStart) -$sliceStart;
	$useThisRegion = 1;
#	print Dumper "3rd $other aln is $alignmentFolder det start $detStartFromUser det end $detEndFromUser start $refStart end $refEnd";
    }
    elsif (($detStartFromUser < $refStart) && ($detEndFromUser < $refEnd) && ($detEndFromUser > $refStart)) {
#	$sliceStart = $detStartFromUser;
#	$sliceEnd =$refEnd;
	$sliceStart = 1;
	$sliceEnd = $detEndFromUser-$refStart;
	$useThisRegion = 1;
#	print Dumper "4th $other aln is $alignmentFolder det start $detStartFromUser det end $detEndFromUser start $refStart end $refEnd";
    }
    else {
	$sliceStart = "nothing";
	$sliceEnd = "nothing";
	$useThisRegion = 0;
    }
    return ($sliceStart,$sliceEnd,$useThisRegion);
}

sub getReferenceSequence {
    my ($project, $contig, $referenceGenome, $start, $stop, $dbh)= @_;
    my $sql= "select SEQUENCE from 
dots.nasequence
where source_id = ?";
    my $sth = $dbh->prepare($sql);
    $sth->execute($contig);
    
    my $sequence;
    if(my @a = $sth->fetchrow_array()) {
	$sequence = $a[0];
    }
    else {
	&userError("Invalid source ID:  $contig\n");
    }
    
    $sth->finish();
    my $length = $stop-$start;
    my $newStart = ($start-1);
#    my $newlength = $length;
    my $substring = substr($sequence, $newStart, $length);
#    print Dumper "seq is \n $sequence \n";
#    print Dumper "sub is \n $substring \n ";
    return $substring;
}

sub makeAgpMap {
    my ($pairs, $pair, $elements) = @_;
    my $file = $pairs."/".$elements.".agp";
    my %agpHash;
    my @backwardAgpArray;
    my @coordinateCheck;
    my $count = 0;
    open(IN, "$file") or die "cant open $file for reading\n";
    while (my $line = <IN>) {
	chomp $line;
#	print Dumper "getting into open agp file\n";
	my @temps = split "\t", $line;
	if ($temps[4] eq 'N') {
	    next;
	}
	else {
#	    print Dumper "getting to the splitting stage\n";
	    my $virtualSourceId = $temps[0];
	    my $virtualStart = $temps[1];
	    my $virtualEnd = $temps[2];
	    my $virtualStrand = $temps[8];
	    my $source_id = $temps[5];
	    my $start = $temps[6];
	    my $end = $temps[7];
	    my $ctg = Bio::Location::Simple->new( -seq_id => $source_id,
						  -start => $start,
						  -end =>  $end,
						  -strand => $virtualStrand);
	    
	    my $ctg_on_chr = Bio::Location::Simple->new( -seq_id =>  $virtualSourceId,
							 -start => $virtualStart,
							 -end =>  $virtualEnd,
							 -strand => '+1' );
	    
#	    my $ctg2 = Bio::Location::Simple->new( -seq_id => $source_id,
#						  -start => $start,
#						  -end =>  $end,
#						  -strand => $virtualStrand );
	    
	    
	    my $agp = Bio::Coordinate::Pair->new( -in  => $ctg, -out => $ctg_on_chr );
	    my $agp2 = Bio::Coordinate::Pair->new( -in => $ctg_on_chr, -out => $ctg);
	    my $pieceSourceId = $source_id;

	    #my $agp2 = Bio::Coordinate::Pair->new( -in =>$ctg_on_chr, -out =>$ctg);
	    #my $pieceSourceId2 = $virtualSourceId;
#	    print Dumper "agp is";
#		    print Dumper $agp2;
	    if (exists $agpHash{$pieceSourceId}){
		print Dumper "ERROR AGP MAP WRONG IN SUB MAKEAGPMAP";
	    }
	    else {
		$agpHash{$pieceSourceId}=$agp;
	    }
	    my %back;
	    my %fullCoord;
	    $back{$virtualSourceId}=$agp2;
	    push (@backwardAgpArray, \%back);
	    $fullCoord{$virtualSourceId}=($virtualStart."-".$virtualEnd."-".$source_id);
	    push (@coordinateCheck, \%fullCoord);
	}
	
    }
    return (\%agpHash,\@backwardAgpArray, \@coordinateCheck);
}

sub reverseCompliment {
    my ($seq) = @_;
#    print Dumper "seq";
#    print Dumper $seq;
    
    my$revcomp = reverse($seq);
#    print Dumper "reverse";
#    print Dumper $revcomp;
    $revcomp=~tr/ACGTacgt/TGCAtgca/;
#    print Dumper "reverse comp:";
#    print Dumper $revcomp;
    return $revcomp;
}

######I AM UP TO HERE AND THIS BUT SHOULD BE OK BUT I NEED TO JUST CHECK EVERYTHING ###############


sub workOutSeq {
#removed slive start and end 
    my ($revComp,%newHash) = @_;
    my %originsHash;
    my %sequenceHash;
#    my @alignments;
    foreach my $element (keys %newHash) {
	my ($seqonly,$newStart,$newEnd,$refStrand,$otherStrand) = @{$newHash{$element}};
	###### for my motes below new start has replaced startToPull
	
	my $uniqueid = $element;
	
	if ($revComp eq 'on') {
	    if ($refStrand eq '+') {
		if ($otherStrand eq '+') {
		    $seqonly = &reverseCompliment($seqonly);
		    my $negseqStart = (-1 * $newStart);
		    $originsHash{$uniqueid}=$negseqStart;
		    
		}
		elsif ($otherStrand eq '-') {
		    $seqonly = &reverseCompliment($seqonly);
		    $originsHash{$uniqueid}=$newStart;
		}
		else {
		    print Dumper "ERROR OTHER STRAND NOT DETERMINED";
		}
	    }
	    elsif($refStrand eq '-') {
		if($otherStrand eq '+') {
		    $originsHash{$uniqueid}=$newStart;
		}
		elsif($otherStrand eq '-') {
		    my $negseqStart = (-1 * $newStart);
		    $originsHash{$uniqueid}=$negseqStart;
		}
		else {
		    print Dumper "ERROR OTHER STRAND NOT DETERMINED";
		}
	    }
	    else{
		print Dumper "ERROR REFERENCE STRAND CAN NOT BE DETERMINED";
	    }
	}
	elsif($revComp ne 'on') {
	    if ($refStrand eq '+') {
		if ($otherStrand eq '+') {
		    $originsHash{$uniqueid}=$newStart;
		}
		elsif($otherStrand eq '-') {
		    my $negseqStart = (-1 * $newStart);
		    $originsHash{$uniqueid}=$negseqStart;
		}
		else {
		    print Dumper "ERROR OTHER STRAND NOT DETERMINED";
		}
	    }
	    elsif($refStrand eq '-') {
		if ($otherStrand eq '+') {
		    $seqonly = &reverseCompliment($seqonly);
		    $originsHash{$uniqueid}=$newStart;
		}
		elsif($otherStrand eq '-') {
		    $seqonly = &reverseCompliment($seqonly);
		    my $negseqStart = (-1 * $newStart);
		    $originsHash{$uniqueid}=$negseqStart;
		}
		else {
		    print Dumper "ERROR OTHER STRAND NOT DETERMINED";
		}
	    }
	    else {
		print Dumper "ERROR REF STRAND CAN NOT BE DETERMINED";
	    }
	}
	$sequenceHash{$uniqueid}=$seqonly;
#	my @alignArray = ($uniqueid,$sliceStart,$sliceEnd,$newStart,$newEnd);
#	push (@alignments, \@alignArray);
    }
#    print Dumper "WITHIN";
 #   print Dumper %sequenceHash;
    return(\%sequenceHash, \%originsHash)
	
}

sub determineSplitSeq {
    my %hash = @_;
    my %newHash;
    my $numberSeq = scalar keys %hash;
    my %multipleHash;
    my $seqToSplit;
    my $length;
    foreach my $element (keys %hash) {
	my ($seqonly,$assStart,$assEnd,$startToPull,$endToPull,$startContig,$endContig,$refStrand,$otherStrand) = @{$hash{$element}}; 
	
	$length = length($seqonly);
	$seqToSplit = $seqonly;
	my ($newStart,$newEnd,$newseq);
	if($assStart<$startToPull) {
	    $newStart = ($startToPull - $assStart);
	}
	elsif($assStart >= $startToPull) {
	    $newStart = 1;
	}
	else {
	    print Dumper "Error cant determind new start";
	}
	if($assEnd <=$endToPull) {
	    $newEnd = $endContig;
	}
	elsif($assEnd >$endToPull) {
	    $newEnd = $endContig-($assEnd-$endToPull)
	}
	else {
	    print Dumper "cant determine new end";
	}
	if ($numberSeq ==1) {
#	    print Dumper "getting into onlt 1 in hash";
	    @{$newHash{$element}}=($seqonly,$newStart,$newEnd,$refStrand,$otherStrand);
#	    print Dumper " new start end $newStart $newEnd seq $seqonly";
	}
	elsif ($numberSeq >1) {
	    @{$multipleHash{$assStart}}=($element,$newStart,$newEnd,$refStrand,$otherStrand,$seqonly);	    
	}
	else {
	    print Dumper "ERROR THERE ISNT ANY MATCHES BEEN SET FOR NUMBERING";
	}	
    }
    if (defined %multipleHash) {
	#DO SEQ MAGIC HERE
	my $count = 0;
#So I know the Ass stuff and I need to order this to know how to split the sequence up. 
	foreach (sort { $a <=> $b } keys(%multipleHash)){
	    my ($element,$newStart,$newEnd,$refStrand,$otherStrand,$seqonly) = @{$multipleHash{$_}};
	    my $snippetLength = ($newEnd-$newStart);
#	    print Dumper "new start nd end $newStart $newEnd";
#	    print Dumper "new length is $snippetLength Length original is $length";
	    if ($length == $snippetLength) {
		print Dumper "ERROR the full length of alignment matches length of the fragment";
	    }
	    else {
		my $newSeq = substr $seqonly, $count, $snippetLength;
#		print Dumper "seq $newSeq";
		$count += $snippetLength;
		@{$newHash{$element}}=($newSeq,$newStart,$newEnd,$refStrand,$otherStrand);
		
	    }
	}
	
	
    }
    return (%newHash);
} 
1;


