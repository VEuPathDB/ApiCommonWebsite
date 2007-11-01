package ApiCommonWebsite::View::CgiApp::MultipleAlignmentMercatorMavid;
use base qw( ApiCommonWebsite::View::CgiApp );

use strict;

use Bio::SeqIO;
use Bio::Seq;

sub new {
  my $self = shift()->SUPER::new();

  my ($mercatorOutputDir, $cndsrcBin) = @_;

  $self->{mercator_output_dir} = $mercatorOutputDir;
  $self->{cndsrc_bin} = $cndsrcBin;
  
  return $self;
}

#--------------------------------------------------------------------------------

sub getMercatorOutputDir {$_[0]->{mercator_output_dir}}
sub getCndsrcBin {$_[0]->{cndsrc_bin}}

#--------------------------------------------------------------------------------

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  print STDOUT $cgi->header('text/plain');

  my ($contig, $start, $stop, $strand, $type) = validateParams($cgi, $dbh);
  my ($agpDir, $alignDir, $sliceAlign, $fa2clustal) = $self->validateMacros();

  my ($genome, $assembly, $assemblyStart, $assemblyStop, $assemblyStrand) = &translateCoordinates($contig, $agpDir, $start, $stop, $strand);
  my ($mapStart, $mapStop) = &validateMapCoordinates($genome, $alignDir, $assembly, $assemblyStart, $assemblyStop);

  if($mapStart && $mapStop) {
    print STDOUT "The Genomic Coordinates provided fall outside a mapped region!\n\n";
    print STDOUT "$contig is mapped between $mapStart and $mapStop\n";
    exit(0);
  }

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
      my $clustal = &makeClustal($fa2clustal, $multiFasta, $cgi, $genome);
  }
  else {
      print STDOUT $multiFasta;
  }
  exit(0);
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
  my ($self) = @_;

  my $mercatorOutputDir = $self->getMercatorOutputDir();
  my $alignmentsDir = "$mercatorOutputDir/alignments";

  my $cndsrcBin = $self->getCndsrcBin();
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
FROM (SELECT source_id
      FROM dots.ExternalNaSequence
      UNION
      SELECT source_id
      FROM dots.VirtualSequence) s
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
  my ($fa2clustal, $multiFasta, $cgi, $genome) =  @_;

  my $rv;

  # Print the deflines on top of the clustal output
  my @lines = split(/\n/, $multiFasta);

  my ($actualStart, $actualStop, $actualStrand);

  foreach my $line (@lines) {
    if($line =~ s/^>//) {
      print STDOUT "$line\n";

      if($line =~ /^$genome [\w\d]+:(\d+)-(\d+)\(([+-])\)/) {
        $actualStart = $1;
        $actualStop = $2;
        $actualStrand = $3;

        $actualStart-- if($actualStrand eq '+');
        $actualStop++ if($actualStrand eq '-');
      }
    }
  }

  print STDOUT "\n";

  # Should try to capture the output of fa2clustal and add numbers but...this doesn't work for > 30,000 bases
  #my $command = "perl -e 'print \"$multiFasta\"'|$fa2clustal";
  #my $clustal = `$command`;
  #my $clustalMod = &addPositions($clustal, $actualStart, $actualStop, $actualStrand, $genome);

  # This is a bit of a hack because I couldn't get the stuff above to work
  my $perlCommand = "perl -e 'my \$start=$actualStart; 
                                my \$stop=$actualStop;
                                my \$strand=\"$actualStrand\";
                                while(<>) {
                                  chomp;
                                  if(/^$genome\\s+(.+)\$/) {
                                    my \$seq = \$1;
                                    my \$n = length \$seq;
                                    my \$nGaps = \$seq =~ tr/-/ /;
                                    my \$offset = \$n - \$nGaps;
                                   if(\$strand eq \"-\" && \$offset > 0) {
                                     \$stop = \$stop - \$offset;
                                     print \$_ . \" \$stop\\n\";
                                   }
                                   elsif(\$strand eq \"+\" && \$offset > 0) {
                                     \$start = \$start + \$offset;
                                     print \$_ . \" \$start\\n\";
                                   }
                                   else {
                                     print \"\$_\\n\";
                                   }
                                 }
                                 else {
                                   print \"\$_\\n\";
                                 }
                               }'";

  #    print STDERR $perlCommand;
  open PIPE, "|$fa2clustal|$perlCommand" or die "Cannot open pipe:$!";
  print PIPE $multiFasta;
  close PIPE;
}

#--------------------------------------------------------------------------------

sub addPositions {
  my ($clustal, $start, $stop, $strand, $genome) = @_;

  my @lines = split(/\n/, $clustal);

  for(my $i = 0; $i < scalar(@lines); $i++) {
    my $line = $lines[$i];

    next unless($line =~ /^$genome\s+(.+)$/);
    my $seq = $1;

    my $n = length $seq;
    my $nGaps = $seq =~ tr/-/ /;

    my $offset = $n - $nGaps;
    next if($offset == 0);

    if($strand eq '-') {
      $stop = $stop - $offset;
      $lines[$i] = "$lines[$i]  $stop";
    }
    else {
      $start = $start + $offset;
      $lines[$i] = "$lines[$i]  $start";
    }
  }

  return join("\n", @lines);
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

  print "ERROR: $msg\n\n";
  exit(1);
}

1;
