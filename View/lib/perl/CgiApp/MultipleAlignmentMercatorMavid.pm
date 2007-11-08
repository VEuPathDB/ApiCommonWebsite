package ApiCommonWebsite::View::CgiApp::MultipleAlignmentMercatorMavid;
use base qw( ApiCommonWebsite::View::CgiApp );

use strict;

use Bio::SeqIO;
use Bio::Seq;

use ApiCommonWebsite::Model::ModelProp;

#--------------------------------------------------------------------------------

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  my ($contig, $start, $stop, $strand, $type) = &validateParams($cgi, $dbh);
  my ($agpDir, $alignDir, $sliceAlign, $fa2clustal) = &validateMacros($cgi);

  my ($genome, $assembly, $assemblyStart, $assemblyStop, $assemblyStrand) = &translateCoordinates($contig, $agpDir, $start, $stop, $strand);
  my ($mapStart, $mapStop) = &validateMapCoordinates($genome, $alignDir, $assembly, $assemblyStart, $assemblyStop);

  if($mapStart && $mapStop) {
    &error("Your Genomic Coordinates fall outside a mapped region!\n\n$contig is mapped between $mapStart and $mapStop");
  }

  &createHeader($cgi, $type, $genome, $contig, $start, $stop, $strand);

  my $multiFasta = makeAlignment($alignDir, $agpDir, $sliceAlign, $genome, $assembly, $assemblyStart, $assemblyStop, $assemblyStrand);

  if($type eq 'fasta_ungapped') {
      my $seqs = &makeUngappedSeqs($multiFasta);

      my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');
      foreach my $seq(@$seqs) {
	  $seqIO->write_seq($seq);
      }
      $seqIO->close();
  }
  elsif($type eq 'clustal') {
    my $clustal = &makeClustal($cgi, $multiFasta, $genome);
    print STDOUT $cgi->end_html();
  }
  else {
    print STDOUT $multiFasta;
  }
  exit(0);
}

#--------------------------------------------------------------------------------

sub createHeader {
  my ($cgi, $type, $genome, $contig, $start, $stop, $strand) = @_;

  my $title = "mercator-MAVID $genome $contig$start-$stop($strand)";

  if($type eq 'clustal') {
    # little style sheet for coloring the html
    my @css = <DATA>;

    print STDOUT $cgi->header();
    print STDOUT $cgi->start_html(-title => $title,
                                  -style  => {-code => join('', @css)},
                                 );
  }
  else {
    print STDOUT $cgi->header('text/plain');
  }
}

#--------------------------------------------------------------------------------

sub translateCoordinates {
  my ($contig, $agpDir, $start, $stop, $strand) = @_;

  opendir(DIR, $agpDir) or &error("Could not open directory $agpDir for reading:$!");

  my ($genome, $assembly);

  while (defined (my $fn = readdir DIR) ) {
    next unless($fn =~ /([\w\d_]+)\.agp$/);

    my $thisGenome = $1;

    open(AGP, "$agpDir/$fn") or &error("Cannot open file $fn for reading: $!");

    while(<AGP>) {
      chomp;
      my @a = split(/\t/, $_);
      my $assemblyName = $a[0];
      my $assemblyStart = $a[1];
      my $assemblyStop = $a[2];
      my $contigName = $a[5];
      my $contigStart = $a[6];
      my $contigStop = $a[7];
      my $contigStrand = $a[8];

      next unless($contigName eq $contig);

      if($genome) {
        &error("Source_id $contig was found in multiple genomes: $genome and $thisGenome");
      }
      $genome = $thisGenome;
      $assembly = $assemblyName;

      if($start > $contigStop || $stop < $contigStart) {
        &error("Please enter coordinates between $contigStart-$contigStop for $contig");
      }

      # The -1 is because sliceAlign has a 1 off error
      if($contigStrand eq '+') {
        $start = $assemblyStart + ($start - $contigStart) - 1;
        $stop = $assemblyStop - ($contigStop - $stop);
      }
      else {
        my $tmpStop = $stop;
        $stop = $assemblyStop - ($start - $contigStart);
        $start = $assemblyStart +  ($contigStop - $tmpStop) -  1;
        $strand = $strand eq '+' ? '-' : '+';
      }
    }
    close AGP;
  }
  close DIR;

  unless($genome) {
    &error("Genome not found for source_id $contig");
  }
  return($genome, $assembly, $start, $stop, $strand);
}

#--------------------------------------------------------------------------------

sub validateMapCoordinates {
  my ($genome, $alignDir, $query, $start, $stop) = @_;

  my $mapfile = "$alignDir/map";
  my $genomesFile = "$alignDir/genomes";

  unless(-e $mapfile) {
    &error("Map file $mapfile does not exist");
  }

  unless(-e $genomesFile) {
    &error("Genomes file $genomesFile does not exist");
  }

  my $index;
  open(GENOME, $genomesFile) or &error("Cannot open file $genomesFile for reading: $!");
  my $line = <GENOME>;
  chomp $line;
  my @genomes = split(/\t/, $line);
  for(my $i = 0; $i < scalar(@genomes); $i++) {
    $index = ($i + 1) * 4 if($genomes[$i] eq $genome);
  }

  close GENOME;

  open(MAP, $mapfile) or error("Cannot open file $mapfile for reading: $!");

  my %mapped;

  while(<MAP>) {
    chomp;

    my @a = split(/\t/, $_);

    my $contig = $a[$index - 3];
    my $mapStart = $a[$index - 2];
    my $mapStop = $a[$index - 1];

    if(my $hash = $mapped{$contig}) {
      $mapped{$contig}->{start} = $hash->{start} < $mapStart ? $hash->{start} : $mapStart;
      $mapped{$contig}->{stop} = $hash->{stop} > $mapStop ? $hash->{stop} : $mapStop;
    }
    else {
      $mapped{$contig} = {start => $mapStart, stop => $mapStop};
    }
  }
  close MAP;

  unless($mapped{$query}) {
    &error("The coordinates $start-$stop fall outside a mapped region.");
  }

  my $mapStart = $mapped{$query}->{start};
  my $mapStop = $mapped{$query}->{stop};

  if($start > $mapStop || $stop < $mapStart) {
    return($mapStart, $mapStop);
  }
}

#--------------------------------------------------------------------------------

sub validateMacros {
  my ($cgi) = @_;

  my $project = $cgi->param('project_id');
  my $props =  ApiCommonWebsite::Model::ModelProp->new($project);
  my $mercatorOutputDir = $props->{MERCATOR_OUTPUT_DIR};
  my $cndsrcBin =  $props->{CNDSRC_BIN};

  my $alignmentsDir = "$mercatorOutputDir/alignments";
  my $sliceAlignment = "$cndsrcBin/sliceAlignment";
  my $fa2clustal = "$cndsrcBin/fa2clustal";

  unless(-e $cndsrcBin) {
    error("cndsrc Bin directory does not exist [$cndsrcBin]");
  }
  unless(-e $sliceAlignment) {
    error("sliceAlignment exe does not exist [$sliceAlignment]");
  }
  unless(-e $fa2clustal) {
    error("fa2clustal exe does not exist [$fa2clustal]");
  }

  unless(-e $alignmentsDir) {
    error("alignments directory not found");
  }

  return($mercatorOutputDir, $alignmentsDir, $sliceAlignment, $fa2clustal);
}

#--------------------------------------------------------------------------------

sub validateParams {
  my ($cgi, $dbh) = @_;

  my $contig       = $cgi->param('contig');
  my $start        = $cgi->param('start');
  my $stop         = $cgi->param('stop');
  my $revComp      = $cgi->param('revComp');
  my $type         = $cgi->param('type');

  &validateContig($contig, $dbh);

  my $strand;
  if($revComp eq 'on') {
      $strand = '-';
  }
  else {
    $strand = '+';
  }

  unless($type eq 'clustal' || $type eq 'fasta_gapped' || $type eq 'fasta_ungapped') {
      &error("Invalid Type [$type]... expected clustal,fasta_gapped,fastaungapped");
  }

  $start =~ s/[,.+\s]//g;
  $stop =~ s/[,.+\s]//g;

  $start = 1 if (!$start || $start !~/\S/);
  $stop = 1000000 if (!$stop || $stop !~ /\S/);
  &error("Start '$start' must be a number") unless $start =~ /^\d+$/;
  &error("End '$stop' must be a number") unless $stop =~ /^\d+$/;
  if ($start < 1 || $stop < 1 || $stop <= $start) {
    &error("Start and End must be positive, and Start must be less than End");
  }
  return ($contig, $start, $stop, $strand, $type);
}

#--------------------------------------------------------------------------------

sub validateContig {
  my ($contig, $dbh) = @_;

  my $sql = <<EOSQL;
SELECT s.source_id 
FROM dots.NaSequence s
WHERE  upper(s.source_id) = ?
EOSQL

  my $sth = $dbh->prepare($sql);
  $sth->execute(uc($contig));

  unless(my ($id) = $sth->fetchrow_array()) {
    &error("Invalid source ID:  $contig\n");
  }
  $sth->finish();

  return $contig;
}

#--------------------------------------------------------------------------------

sub replaceAssembled {
  my ($agpDir, $genome, $input, $start, $stop, $strand) = @_;

  my $fn = "$agpDir/$genome" . ".agp";

  open(FILE, $fn) or error("Cannot open file $fn for reading:$!");

  my @v;

  while(<FILE>) {
    chomp;

    my @ar = split(/\t/, $_);

    my $assembly = $ar[0];
    my $assemblyStart = $ar[1];
    my $assemblyStop = $ar[2];
    my $type = $ar[4];

    my $contig = $ar[5];
    my $contigStart = $ar[6];
    my $contigStop = $ar[7];
    my $contigStrand = $ar[8];

    next unless($type eq 'D');
    my $shift = $assemblyStart - $contigStart;
    my $checkShift = $assemblyStop - $contigStop;

    &error("Cannot determine shift") unless($shift == $checkShift);

    if($assembly eq $input && 
       (($start >= $assemblyStart && $start <= $assemblyStop) || ($stop >= $assemblyStart && $stop <= $assemblyStop))) {
      
      my ($newStart, $newStop, $newStrand);

      # the +1 and -1 is because of a 1 off error in the sliceAlign program
      if($contigStrand eq '+') {
        $newStart = $start < $assemblyStart ? $contigStart : $start - $assemblyStart + $contigStart + 1;
        $newStop = $stop > $assemblyStop ? $contigStop : $stop - $assemblyStart + $contigStart; 
        $newStrand = $strand;
      }
      else {
        $newStart = $start < $assemblyStart ? $contigStop : $assemblyStop - $start + $contigStart - 1;
        $newStop = $stop > $assemblyStop ? $contigStart : $assemblyStop - $stop + $contigStart;  
        $newStrand = $strand eq '+' ? '-' : '+';
      }

      if($newStart <= $newStop) {
        push(@v, "$contig:$newStart-$newStop($newStrand)");
      }
      else {
        push(@v, "$contig:$newStop-$newStart($newStrand)");
      }
    }
  }
  close FILE;

  return ">$genome " . join(';', @v);
}

#--------------------------------------------------------------------------------

sub makeAlignment {
  my ($alignDir, $agpDir, $sliceAlign, $referenceGenome, $queryContig, $queryStart, $queryStop, $queryStrand) = @_;

  my $command = "$sliceAlign $alignDir $referenceGenome '$queryContig' $queryStart $queryStop $queryStrand";

  my $alignments = `$command`;

  my @lines = split(/\n/, $alignments);
  for(my $i = 0; $i < scalar (@lines); $i++) {
    my $line = $lines[$i];

    my ($genome, $assembled, $start, $stop, $strand) = $line =~ />([a-zA-Z1-9_]+) ([\w\d]+):(\d+)-(\d+)([-+])/;
    next unless($genome);
    my $replaced = &replaceAssembled($agpDir, $genome, $assembled, $start, $stop, $strand);

    $lines[$i] = $replaced;

    if($line =~ />/) {
      $lines[$i] =~ s/([+|-])$/\($1\)/;
    }
  }

  return join("\n", @lines) . "\n";
}

#--------------------------------------------------------------------------------

sub makeClustal {
  my ($cgi, $multiFasta, $referenceGenome) =  @_;

  my @lines = split(/\n/, $multiFasta);

  my ($start, $stop, $strand, $thisGenome, $count);

  my (%allSequences, @genomes);

  foreach my $line (@lines) {
    next unless($line);
    if($line =~ /^>(([\w\d_]+) [\w\d]+:(\d+)-(\d+)\(([+-])\))/ || $line =~ /^>(.+)/) {
      $thisGenome = $1;
      push(@genomes, $thisGenome);

      my $genome = $2;
      if($genome eq $referenceGenome) {
        $start = $3 - 1;;
        $stop = $4 + 1;
        $strand = $5;
      }

      print STDOUT "$line</br>\n";
    }
    else {
      push(@{$allSequences{$thisGenome}}, $line);
      $count++
    }
  }
  $count = $count / scalar(keys(%allSequences));
  print STDOUT "</br>\n";

  &printClustal(\%allSequences, $start, $stop, $strand, $referenceGenome, $count, \@genomes);
}

#--------------------------------------------------------------------------------

sub printClustal {
  my ($allSequences, $start, $stop, $strand, $referenceGenome, $count, $genomes) = @_;

  my @genomes = @$genomes;
  my $colWidth = 15;

  my $referenceCursor;

  for my $i (0..$count-1) {
    my %sequenceLines = ();
    foreach my $genome (@genomes) {

      my @allLines = @{$allSequences->{$genome}};
      $sequenceLines{$genome} = $allLines[$i];

      # Keep track of the Reference Genome Positions
      if($genome =~ /$referenceGenome/) {
        my $seq = $allLines[$i];
        my $n = length $seq;
        my $nGaps = $seq =~ tr/-/ /;

        my $offset = $n - $nGaps;
        next if($offset == 0);

        if($strand eq '-') {
          $stop = $stop - $offset;
          $referenceCursor = $stop;
        }
        else {
          $start = $start + $offset;
          $referenceCursor = $start;
        }
      }
    }

    my $markup = &markupSequences(\%sequenceLines, $referenceGenome);

    foreach my $genome (@genomes) {
      $genome =~ /^([\w\d_]+)/;
      my @genomeChars = split('', $1);
      for(0..$colWidth) {
        my $char = defined($genomeChars[$_]) ? $genomeChars[$_] : '&nbsp';
        print STDOUT $1 eq $referenceGenome ? "<b class=\"maroon\">$char</b>" : $char;
      }

      if($genome =~ /$referenceGenome/) {
        print STDOUT $markup->{$genome}. " $referenceCursor". "</br>";
      }
      else {
        print STDOUT $markup->{$genome}."</br>";
      }
    }
    print STDOUT "</br>";
  }

}

#--------------------------------------------------------------------------------

sub markupSequences {
  my ($sequences, $reference) = @_;

  my (@referenceBases, %positions, %markedUpSequences);

  # find the reference first
  foreach my $genome (keys %$sequences) {
    next unless($genome =~ /^$reference/);

    @referenceBases = split('', $sequences->{$genome});
  }

  # find the positions where the non reference differ from the reference
  foreach my $genome (keys %$sequences) {
    next if($genome =~ /^$reference/);

    my @nonRefBases = split('', $sequences->{$genome});

    unless(scalar @referenceBases == scalar @nonRefBases) {
      &error("Error in the number of bases for $genome");
    }
    for(my $i = 0; $i < scalar(@nonRefBases); $i++) {
      next if($nonRefBases[$i] eq $referenceBases[$i] || $nonRefBases[$i] eq '-' || $referenceBases[$i] eq '-');
      $positions{$i} = 1;
    }
  }

  # if there was a difference... change all the colors
  foreach my $genome (keys %$sequences) {
    my @bases = split('', $sequences->{$genome});

    for(my $i = 0; $i < scalar(@bases); $i++) {
      $bases[$i] = "<b class=\"red\">$bases[$i]</b>" if($positions{$i});
    }
    $markedUpSequences{$genome} = join('', @bases);
  }

  return \%markedUpSequences;
}

#--------------------------------------------------------------------------------

sub makeUngappedSeqs {
  my ($multiFasta) = @_;

  my @lines = split(/\n/, $multiFasta);

  my @rv;

  my ($defline, $seq, $start);
  foreach my $line (@lines) {
    if($line =~ /^>/) {
      if($start) {
        my $bioSeq = Bio::Seq->new(-description => $defline, 
                                   -seq => $seq,
                                   -alphabet => "dna");
        push(@rv, $bioSeq);
      }
      $start = 1;
      $defline = $line;
      $defline =~ s/^>//;
      $seq = "";
    }
    else {
      $line =~ s/-//g;
      $seq = $seq . $line;
    }
  }
  # dont' forget the last one...
  my $bioSeq = Bio::Seq->new(-display_id => $defline, 
                             -seq => $seq,
                             -alphabet => "dna");
  push(@rv, $bioSeq);
  return \@rv;
}

#--------------------------------------------------------------------------------

sub error {
  my ($msg) = @_;

  die "ERROR: $msg\n\n";
}

1;

__DATA__
body
{
font-family: courier, 'serif'; 
font-size: 100%;
background-color: #ffffff;
}
b.red
{
font-family: courier, 'serif';
font-weight: normal;
color:#FF0000; 
}
b.maroon
{
font-family: courier, 'serif';
font-weight: normal;
color:#800000; 
}
