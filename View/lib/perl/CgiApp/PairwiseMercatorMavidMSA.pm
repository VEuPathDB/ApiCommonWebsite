package ApiCommonWebsite::View::CgiApp::PairwiseMercatorMavidMSA;
use base qw( ApiCommonWebsite::View::CgiApp );

use strict;

use IO::String;
use Bio::SeqIO;
use Bio::Seq;

use Data::Dumper;

use ApiCommonWebsite::Model::ModelProp;
use EuPathSiteCommon::Model::ModelXML;

use Bio::Graphics::Browser2::PadAlignment;

use CGI::Carp qw(fatalsToBrowser set_message);

sub getSortingGroupsHash {
  my ($reference,$taxonDirHash) = @_;

  my %groupsHash;

  foreach my $sp (keys %$taxonDirHash) {
    my $hash = $taxonDirHash->{$sp};

    my $name = $hash->{name};
    my $group = $hash->{group};

    $groupsHash{$name} = $group;
  }

  my $referenceGroup = $groupsHash{$reference};
  foreach my $name (keys %groupsHash) {
    if($groupsHash{$name} == $referenceGroup) {
      $groupsHash{$name} = 0;
    }
  }

  return \%groupsHash;
}

# ========================================================================
# ----------------------------- BEGIN Block ------------------------------
# ========================================================================
BEGIN {
    # Carp callback for sending fatal messages to browser
    sub handle_errors {
        my ($msg) = @_;
        print "<p><pre>$msg</pre></p>";
    }
    set_message(\&handle_errors);
}

#--------------------------------------------------------------------------------

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  my $taxonToDirNameMap = getTaxonToDirMap($cgi, $dbh);

  my ($contig, $start, $stop, $strand, $type, $referenceGenome, $genomes) = &validateParams($cgi, $dbh,$taxonToDirNameMap);
  my ($mercatorOutputDir, $sliceAlign, $fa2clustal, $pairwiseDirectories, $availableGenomes) = &validateMacros($cgi);

  &createHeader($cgi, $type);

  my @alignments;
  my @dnaSequences;

  # foreach pairwise comparison
  foreach my $otherGenome (@$genomes) {
    if($otherGenome eq $referenceGenome) {
      next;
    }

    my $agpDir = &findDirectory($referenceGenome, $otherGenome, $pairwiseDirectories);
    my $alignDir = "$agpDir/alignments";

    # if we ran mercator as draft ... we need to translate into assemblies to run sliceAlign

    my ($genome, $assembly, $assemblyStart, $assemblyStop, $assemblyStrand) = &translateCoordinates($contig, $agpDir, $start, $stop, $strand);

    eval {
      &validateMapCoordinates($genome, $alignDir, $assembly, $assemblyStart, $assemblyStop, $agpDir);
    };
    if($@) {
      print STDOUT $@; 
      next;
    }

    my $multiFasta = makeAlignment($alignDir, $agpDir, $sliceAlign, $genome, $assembly, $assemblyStart, $assemblyStop, $assemblyStrand);

    &makePadAlignmentInputFromMultiFasta($multiFasta, \@dnaSequences, \@alignments, $referenceGenome);
  }

  my $dnas = &makeSequencesForPadAlignment(\@dnaSequences, $referenceGenome, $taxonToDirNameMap);

  my $align = Bio::Graphics::Browser2::PadAlignment->new($dnas,\@alignments);

  if($type eq 'fasta_ungapped') {
    my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

    my $seenReference;
    foreach my $seq (@dnaSequences) {
      my $id = $seq->id();
      next if($seenReference and $id eq $referenceGenome);
      if($id eq $referenceGenome) {
        $seenReference++;
      }

      my $sequence = $seq->seq();
      $sequence =~ s/-//g;

      my $ungappedSeq = Bio::Seq->new( -seq => $sequence,
                                       -id  => $id
                                     );

      $seqIO->write_seq($ungappedSeq);
    }
    $seqIO->close();
  }

  if($type eq 'clustal') {
    my $locationTable = &getLocationsFromDefline($cgi, \@dnaSequences, $referenceGenome);
    my $referenceStart = {};

    my $sortingGroupsHash = &getSortingGroupsHash($referenceGenome,$taxonToDirNameMap);

    print STDOUT "<br /><table border=1 cellpadding='6px' RULES=GROUPS FRAMES=BOX valign='left'>\n";
    print STDOUT"<tr><thead align='left'><th>Genome</th><th>Sequence</th><th>Start</th><th>End</th><th>Strand</th><th>#Nucleotides</th></tr></thead>\n";

    foreach my $g (sort {$sortingGroupsHash->{$a} <=> $sortingGroupsHash->{$b}} keys %$locationTable) {
      my $row = $locationTable->{$g};

      my $_sequence = $row->{sequence};
      my $_start = $row->{start};
      my $_stop = $row->{stop};
      my $_strand = $row->{strand};
      my $_length = $row->{length};

      print STDOUT"<tr><td>$g</td><td>$_sequence</td><td>$_start</td><td>$_stop</td><td>$_strand</td><td>$_length</td></tr>\n";

      if($_strand eq '-') {
        $referenceStart->{$g} =  -1 * ($_start);
      }
      else {
        $referenceStart->{$g} =  $_start;
      }

    }
    print STDOUT "</table>";

    print STDOUT $cgi->pre($align->alignment( $referenceStart, { show_mismatches   => 1})); 
    $cgi->end_html;
  }

  exit(0);
}

#-------------------------------------------------------------------------------
#Make Organism Hash Map
#-------------------------------------------------------------------------------

sub getTaxonToDirMap {
  my ($cgi,$dbh)  = @_;
  my $taxonToDirMap;

  my $project = $cgi->param('project_id');

     my $sql = <<EOSQL;
SELECT distinct ga.organism, taxon.grp, org.abbrev 
FROM   ApiDBTuning.GeneAttributes ga, ApiDB.Organism org,
       (SELECT organism, row_number() over (order by organism) as grp 
        FROM (SELECT distinct organism FROM ApiDBTuning.GeneAttributes)
       ) taxon 
WHERE  ga.taxon_id = org.taxon_id
AND    ga.gene_type = 'protein coding'
AND    ga.organism = taxon.organism
EOSQL

     my $sth = $dbh->prepare($sql);
     $sth->execute();

     while (my $hashref = $sth->fetchrow_hashref()) {
       $taxonToDirMap->{$hashref->{ORGANISM}} = {name => $hashref->{ABBREV}, group => $hashref->{GRP} };
     }

return $taxonToDirMap;
}

#--------------------------------------------------------------------------------
# Methods for generating PadAlignment
#--------------------------------------------------------------------------------

sub makeSequencesForPadAlignment {
  my ($dnaSequences, $referenceGenome, $taxonToDirNameMap) = @_;

  my $sortingGroupsHash = &getSortingGroupsHash($referenceGenome,$taxonToDirNameMap);
  my $ref = shift @$dnaSequences;

  my @sorted = sort {$sortingGroupsHash->{$a->id()} <=> $sortingGroupsHash->{$b->id()} } @$dnaSequences;
  unshift @sorted, $ref if($ref);

  my @dnas;
  my $seenReference;

  foreach(@sorted) {
    if($_->id eq $referenceGenome) {
      next if $seenReference;
      $seenReference = 1;
    }

    my $seq = $_->seq;

    $seq =~ s/-//g;

    push @dnas, ($_->id => $seq);
  }

  return \@dnas;
}

#--------------------------------------------------------------------------------

sub makePadAlignmentInputFromMultiFasta {
  my ($multiFasta, $dnaSequences, $alignments, $referenceGenome) = @_;

  my $stringfh = new IO::String($multiFasta);
  my $seqio = Bio::SeqIO-> new(-fh     => $stringfh,
                               -format => 'fasta');

  my $seq1 = $seqio->next_seq;
  my $seq2 = $seqio->next_seq;

  $seqio->close();
  $stringfh->close();

  next unless($seq1);

  my $str1 = $seq1->seq();
  my $str2 = $seq2->seq();

  my $gaps1 = &getGapPositions($str1);
  my $gaps2 = &getGapPositions($str2);

  my $unionedSortedGaps = &unionGaps($gaps1, $gaps2);

  # Get the locations where they match between both strings
  my $matches = &getMatchingLocations($unionedSortedGaps);

  my $adjustedLocations1 = &adjustMatches($matches, $gaps1);
  my $adjustedLocations2 = &adjustMatches($matches, $gaps2);

  unless(scalar @$adjustedLocations1 == scalar @$adjustedLocations2) {
    &error("Alignment Error");
  }

  if($seq1->id() eq $referenceGenome) {
    push @$dnaSequences, $seq1, $seq2;
  }
  else {
    push @$dnaSequences, $seq2, $seq1;
  }

  for(my $i = 0; $i < scalar @$adjustedLocations1; $i++) {
    my $loc1 = $adjustedLocations1->[$i];
    my $loc2 = $adjustedLocations2->[$i];

    if($seq1->id() eq $referenceGenome) {
      push @$alignments, [$seq2->id(), @$loc1, @$loc2];
    }
    else {
      push @$alignments, [$seq1->id(), @$loc2, @$loc1];
    }
  }
}

#--------------------------------------------------------------------------------

sub adjustMatches {
  my ($matches, $gaps) = @_;

  my $adjusted = [];

  foreach my $match (@$matches) {
    my $min = $match->[0];
    my $max = $match->[1];

    my $prevGapCount = &countPreviousGaps($gaps, $min);

    my $adjustedMin = $min - $prevGapCount - 1;
    my $adjustedMax = $max - $prevGapCount - 1;

    push @$adjusted, [$adjustedMin, $adjustedMax];
  }

  return $adjusted;
}

#--------------------------------------------------------------------------------

sub countPreviousGaps {
  my ($positions, $index) = @_;

  my $count;
  foreach(@$positions) {
    return $count if($_ >= $index);
    $count++;
  }
  return $count;
}

#--------------------------------------------------------------------------------

# Get the gap positions as an array
sub getGapPositions {
  my ($str) = @_;

  my @a;
  while( $str =~ m/-/g) {
    push @a, pos($str);
  }
  push @a, length($str) + 1;

  return \@a;
}

#--------------------------------------------------------------------------------

sub unionGaps {
  my ($gaps1, $gaps2) = @_;

  my %gapsHash;
  foreach(@$gaps1, @$gaps2) {
    $gapsHash{$_} = 1;
  }
  my @allGaps = sort { $a <=> $b } keys %gapsHash;

  return \@allGaps;
}

#--------------------------------------------------------------------------------

sub getMatchingLocations {
  my ($gaps) = @_;

  my @a = @$gaps;

  my $min = 1;
  my $max;

  my $locations = [];

  for(my $i = 0; $i < scalar @a; $i++) {
    my $gapPos = $a[$i];

    if($gapPos == $min) {
      $min = $gapPos + 1;
      next;
    }

    $max = $gapPos - 1;
    push @$locations, [$min, $max];
    $min = $gapPos + 1;
  }

  return $locations;
}

#--------------------------------------------------------------------------------
# Other methods
#--------------------------------------------------------------------------------

sub findDirectory {
  my ($ref, $genome, $pairwiseDirectories) = @_;

  foreach(@$pairwiseDirectories) {
    if(/$ref(\-|$)/ && /$genome(\-|$)/) {
      return $_;
    }
  }

  &error("Could not find a directory for [$ref] and [$genome].");
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
    next unless($fn =~ /(\S+)\.agp$/);

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
        &userError("Please enter coordinates between $contigStart-$contigStop for $contig");
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
    &userError("$contig was not found in any of the genomes which were input to mercator.\n\nUse the chromosome id for scaffolds which have been assembled into chromosomes");
  }
  return($genome, $assembly, $start, $stop, $strand);
}

#--------------------------------------------------------------------------------

sub validateMapCoordinates {
  my ($genome, $alignDir, $query, $start, $stop, $agpDir) = @_;

  my $mapfile = "$alignDir/map";
  my $genomesFile = "$alignDir/genomes";

  unless(-e $mapfile) {
    &error("Map file $mapfile does not exist");
  }

  unless(-e $genomesFile) {
    &error("Genomes file $genomesFile does not exist");
  }

  my ($index, @genomes);
  open(GENOME, $genomesFile) or &error("Cannot open file $genomesFile for reading: $!");
  my $line = <GENOME>;
  chomp $line;
  @genomes = split(/\t/, $line);
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
    &userError("There is no alignment data available for $genomes[0] vs $genomes[1]:  $query.\n\nGenomic sequences with few or no genes will not be mapped.");
  }

  my $mapStart = $mapped{$query}->{start};
  my $mapStop = $mapped{$query}->{stop};

  if($start >= $mapStop || $stop <= $mapStart || $start < $mapStart || $stop > $mapStop) {
    my $mappedCoord = replaceAssembled($agpDir, $genome, $query, $mapStart, $mapStop, '+');
    my ($junk, $included) = split(' ', $mappedCoord);

    userError("$genomes[0] vs. $genomes[1] didn't align in the region you chose.  Pairwise alignment is only available for:  $included");
  }
}

#--------------------------------------------------------------------------------

sub validateMacros {
  my ($cgi) = @_;

  my $project = $cgi->param('project_id');

  my $props =  ApiCommonWebsite::Model::ModelProp->new($project);
  my $model = EuPathSiteCommon::Model::ModelXML->new('apiCommonModel.xml');

  my $buildNumber = $model->getBuildNumberByProjectId($project);
  my $wsMirror = $props->{WEBSERVICEMIRROR};

  my $mercatorOutputDir = $wsMirror . "/$project/build-$buildNumber/mercator_pairwise/";

  my $cndsrcBin =  $props->{CNDSRC_BIN};

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

  opendir(DIR, $mercatorOutputDir) || die "Can't open $mercatorOutputDir for reading:$!";
  my @pairwiseDirs = grep { -d "$mercatorOutputDir/$_" &&  /[a-zA-Z0-9_-]/ } readdir(DIR);
  closedir DIR;

  my @pairwiseDirectories;


  my %genomesHash;
  foreach my $dir (@pairwiseDirs) {

    my (@genomes) = split("-", $dir);
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

  return($mercatorOutputDir, $sliceAlignment, $fa2clustal, \@pairwiseDirectories, \@availableGenomes);
}

#--------------------------------------------------------------------------------

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



  return ($contig, $start, $stop, $strand, $type, $referenceGenome, \@genomes);
}

#--------------------------------------------------------------------------------

sub getOrganismFromContig {
  my ($contig, $dbh) = @_;

  my $sql = <<EOSQL;
SELECT s.source_id, s.organism
FROM ApidbTuning.GenomicSequenceAttributes s
WHERE  upper(s.source_id) = ?
EOSQL

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
       (($start >= $assemblyStart && $start <= $assemblyStop) || 
        ($stop >= $assemblyStart && $stop <= $assemblyStop) ||
        ($start < $assemblyStart && $stop > $assemblyStop ))) {
      
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

    my ($genome, $assembled, $start, $stop, $strand) = $line =~ />(\S+) (\S+):(\d+)-(\d+)([-+])/;
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

sub getLocationsFromDefline {
  my ($cgi, $sequences, $referenceGenome) =  @_;

  my $rv;

  my ($start, $stop, $strand, $thisGenome, $count, $shortName);

  my (%allSequences, @genomes);

  foreach my $seq (@$sequences) {
    my $desc = $seq->desc();
    my $id = $seq->id();

    if($desc =~ /((\S*?):(\d+)-(\d+)\(([+-])\).*)/) {
      my $genome = $id;
      my $genomicSequence = $2;

      $start = $3;
      $stop = $4;
      $strand =  $5;

      my $matchLength = $stop - $start + 1;

      $rv->{$id} = {sequence => $genomicSequence, 
                    start => $start,
                    stop => $stop,
                    strand => $strand,
                    length => $matchLength,
                   };
#        print STDOUT"<tr><td>$genome</td><td>$tmpSequence</td><td>$tmpStart</td><td>$tmpStop</td><td>$tmpStrand</td><td>$tmpLength</td></tr>\n";
    }
    else {
      $desc =~ /^>(.+)/;
      $thisGenome = $1;

      my $emptyCell = 'N/A';

      $rv->{$id} = {sequence => $emptyCell, 
                    start => $emptyCell, 
                    stop => $emptyCell, 
                    strand => $emptyCell,
                    length => $emptyCell, 
                   };

#      print STDOUT"<tr><td>$thisGenome</td><td>$emptyCell</td><td>$emptyCell</td><td>$emptyCell</td><td>$emptyCell</td><td>$emptyCell</td></tr>\n";
    }

  }
  return $rv;
}

#--------------------------------------------------------------------------------

sub error {
  my ($msg) = @_;

  die "ERROR: $msg\n\nPlease report this error.  \nMake sure to include the error message, contig_id, start and end positions.\n";
}

sub userError {
  my ($msg) = @_;

  die "$msg\n\nPlease Try again!\n";
}

1;
