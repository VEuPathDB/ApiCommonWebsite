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

  my ($ref, $contig, $start, $stop, $strand, $type) = validateParams($cgi, $dbh);
  my ($agpDir, $alignDir, $sliceAlign, $fa2clustal) = $self->validateMacros();

  my $multiFasta = makeAlignment($alignDir, $agpDir, $sliceAlign, $ref, $contig, $start, $stop, $strand);

  if($type eq 'fasta_ungapped') {
      my $seqs = &makeUngappedSeqs($multiFasta);
      
      my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');
      foreach my $seq(@$seqs) {
	  $seqIO->write_seq($seq);
      }
      $seqIO->close();
  }
  elsif($type eq 'clustal') {
      my $clustal = &makeClustal($fa2clustal, $multiFasta, $cgi);
      print STDOUT $clustal;
  }
  else {
      print STDOUT $multiFasta;
  }

  exit(0);
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

  my $genome       = $cgi->param('genome');
  my $contig       = $cgi->param('contig');
  my $start        = $cgi->param('start');
  my $stop         = $cgi->param('stop');
  my $strand       = $cgi->param('strand');
  my $type         = $cgi->param('type');

  unless($genome) {
    error("Reference Gemonome not specified");
  }

  &validateContig($contig, $dbh);

  if($strand eq 'forward') {
      $strand = '+';
  }
  elsif($strand eq 'reverse') {
      $strand = '-';
  }
  else {
    &error("unrecognized strand [$strand]\n");
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
  return ($genome, $contig, $start, $stop, $strand, $type);
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
    #&error("Invalid source ID:  $contig\n");
  }
  $sth->finish();

  return $contig;
}

#--------------------------------------------------------------------------------

sub replaceAssembled {
  my ($agpDir, $genome, $input, $start, $stop, $strand) = @_;

  my $fn = "$agpDir/$genome" . ".agp";

  open(FILE, $fn) or die "Cannot open file $fn for reading:$!";

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

    die "Cannot determine shift" unless($shift == $checkShift);

    if($assembly eq $input && 
       (($start >= $assemblyStart && $start <= $assemblyStop) || ($stop >= $assemblyStart && $stop <= $assemblyStop))) {
      
        my ($newStart, $newStop, $newStrand);

        if($contigStrand eq '+') {
          $newStart = $start < $assemblyStart ? $contigStart : $start - $assemblyStart + $contigStart + 1;
          $newStop = $stop > $assemblyStop ? $contigStop : $stop - $assemblyStart + $contigStart; 
          $newStrand = $contigStrand;
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

    if($line =~ /assembled/) {
      my ($genome, $assembled, $start, $stop, $strand) = $line =~ />([a-zA-Z1-9_]+) (assembled\d+):(\d+)-(\d+)([-+])/;
      my $replaced = &replaceAssembled($agpDir, $genome, $assembled, $start, $stop, $strand);

      $lines[$i] = $replaced;
    }

    if($line =~ />/) {
      $lines[$i] =~ s/([+|-])$/\($1\)/;
    }
  }

  return join("\n", @lines) . "\n";
}

#--------------------------------------------------------------------------------

sub makeClustal {
    my ($fa2clustal, $multiFasta, $cgi) =  @_;

    my $rv;

    my $genome = $cgi->param('genome');

    # Print the deflines on top of the clustal output
    my @lines = split(/\n/, $multiFasta);

    foreach my $line (@lines) {
	if($line =~ s/^>//) {
	    $rv = "$rv$line\n";
	}
    }

    my $command = "echo '$multiFasta'|fa2clustal";

    my $clustal = `$command`;

    return "\n$rv$clustal\n";
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
