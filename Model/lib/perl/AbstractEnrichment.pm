package ApiCommonWebsite::Model::AbstractEnrichment;

use strict;
use DBI;
use File::Basename;
use WDK::Model::ModelConfig;
use IPC::Open2;

sub run {
  my ($self, $outputFile, $geneResultSql, $modelName, $pValueCutoff, $secondOutputFile) = @_;

  my $c = new WDK::Model::ModelConfig($modelName);

  my $dbh = DBI->connect($c->getAppDb->getDbiDsn, $c->getAppDb->getLogin, $c->getAppDb->getPassword) or die DBI::errstr;

  my $taxonId = $self->getTaxonId($dbh, $geneResultSql);

  my $annotatedGenesBgd = $self->getAnnotatedGenesCountBgd($dbh, $taxonId);
  my $annotatedGenesResult = $self->getAnnotatedGenesCountResult($dbh, $geneResultSql);
 my $annotatedGenesListResult = $self->getAnnotatedGenesListResult($dbh, $geneResultSql);

  # get query to get back table to feed to python.
  # the columns are:  goId, bgdGeneCount, resultSetGeneCount
#  my $dataSql = $self->getDataSql($taxonId, $geneResultSql);
  my $dataListSql =$self->getDataListSql($taxonId, $geneResultSql, $dbh);
 # $self->getEnrichment($dbh, $outputFile, $annotatedGenesBgd, $annotatedGenesResult, $dataSql, $pValueCutoff);
  $self->getEnrichmentList($dbh, $secondOutputFile, $annotatedGenesBgd, $annotatedGenesResult, $dataListSql, $pValueCutoff, $annotatedGenesListResult, $outputFile);

  $dbh->disconnect or warn "Disconnection failed: $DBI::errstr\n";
}

sub getAnnotatedGenesCountBgd {
  my ($self, $dbh, $taxonId) = @_;
  die "subclass must override getAnnotatedGenesCountBgd method";
}

sub getAnnotatedGenesCountResult {
  my ($self, $dbh, $geneResultSql) = @_;
  die "subclass must override getAnnotatedGenesCountResult method";
}
sub getAnnotatedGenesListResult {
  my ($self, $dbh, $geneResultSql) = @_;
  die "subclass must override getAnnotatedGenesListResult method";
}

#sub getDataSql {
#  my ($self, $taxonId, $geneResultSql) = @_;
#  die "subclass must override getDataSql method";
#}

sub runSql {
  my ($self, $dbh, $sql) = @_;
  # print STDERR "\n$sql\n\n";

  my $stmt = $dbh->prepare("$sql") or die(DBI::errstr);
  $stmt->execute() or die(DBI::errstr);
  return $stmt;
}

#sub getEnrichment {
#  my ($self, $dbh, $outputFile, $annotatedGenesBgd, $annotatedGenesResult, $dataSql, $pValueCutoff) = @_;

#  my $cmd = "enrichmentAnalysis $pValueCutoff $annotatedGenesBgd $annotatedGenesResult";

#  local (*Reader, *Writer);
#  my $pid = open2(\*Reader, \*Writer, $cmd);

#  my $stmt = $self->runSql($dbh, $dataSql);

#  while ( my($annotationId, $geneCountBgd, $geneCountResult, $pctOfBgd, $annotationName) = $stmt->fetchrow_array()) {
##    print STDERR "$annotationId\t$geneCountBgd\t$geneCountResult\t$pctOfBgd\t$annotationName\n";
#    print Writer "$annotationId\t$geneCountBgd\t$geneCountResult\t$pctOfBgd\t$annotationName\n";
#  }

#  close Writer;

#  open(OUT, ">$outputFile") || die "Can't open '$outputFile' for writing\n";

#  print OUT join("\t", "ID", "Name", "Bgd count", "Result count", "Pct of bgd", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni") . "\n";
#  while(<Reader>) {
#    chomp;
#    my ($foldEnrichment, $oddsRatio, $percentOfResult, $pValue, $benjamini, $bonferroni, $termId, $bgdCount, $resultCount, $pctOfBgd, $annotationName) = split(/\t/);
#    print OUT join("\t", $termId, $annotationName, $bgdCount, $resultCount, $pctOfBgd, $foldEnrichment, $oddsRatio, $pValue, $benjamini, $bonferroni) . "\n";
#  }
#  close Reader;
#  waitpid($pid, 0);
#  my $s = $? >> 8;
#  die "Failed running command '$cmd'" if $s;
#}
sub getEnrichmentList {
  my ($self, $dbh, $secondOutputFile, $annotatedGenesBgd, $annotatedGenesResult, $dataListSql, $pValueCutoff, $annoatedGenesListResult, $outputFile) = @_;

  my $cmd = "enrichmentAnalysis $pValueCutoff $annotatedGenesBgd $annotatedGenesResult";

  local (*Reader, *Writer);
  my $pid = open2(\*Reader, \*Writer, $cmd);

  my $stmt = $self->runSql($dbh, $dataListSql);

  while ( my($annotationId, $geneCountBgd, $geneCountResult, $geneListResult, $pctOfBgd, $annotationName) = $stmt->fetchrow_array()) {
#    print STDERR "$annotationId\t$geneCountBgd\t$geneCountResult\t$pctOfBgd\t$annotationName\n";
    print Writer "$annotationId\t$geneCountBgd\t$geneCountResult\t$geneListResult\t$pctOfBgd\t$annotationName\n";
  }

  close Writer;

  open(OUT, ">$secondOutputFile") || die "Can't open '$secondOutputFile' for writing\n";
  print OUT join("\t", "ID", "Name", "Bgd count", "Result count", "Result gene list","Pct of bgd", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni") . "\n";
  while(<Reader>) {
      chomp;
      my ($foldEnrichment, $oddsRatio, $percentOfResult, $pValue, $benjamini, $bonferroni, $termId, $bgdCount, $resultCount, $resultList, $pctOfBgd, $annotationName) = split(/\t/);
#      print "LIST IS $resultList\n";
      my @templist = split ",", $resultList;
      my $newlist;
      my %list;
      foreach my $element (@templist) {
	  if (exists $list{$element}) {
	      next;
	  }
	  else {
	      $list{$element} = 1;
	      $newlist.= $element.",";
	  }
      }
      print OUT join("\t", $termId, $annotationName, $bgdCount, $resultCount, $newlist, $pctOfBgd, $foldEnrichment, $oddsRatio, $pValue, $benjamini, $bonferroni) . "\n";
  }
  close Reader;
  waitpid($pid, 0);
  my $s = $? >> 8;
  die "Failed running command '$cmd'" if $s;
  close OUT; 
  open(OUT2, ">$outputFile") ||die "Cant open '$outputFile' for writing\n";
  print OUT2 join("\t", "ID", "Name", "Bgd count", "Result count", "Pct of bgd", "Fold enrichment", "Odds ratio", "P-value", "Benjamini", "Bonferroni") . "\n";
  open (IN, "$secondOutputFile") || die "cant open $secondOutputFile for readings\n";
  while (my $line = <IN>) {
      chomp $line;
      if ($line=~/^ID/) {
	  next;
      }
      else{
	  my ($termId, $annotationName, $bgdCount, $resultCount, $resultList, $pctOfBgd, $foldEnrichment, $oddsRatio, $pValue, $benjamini, $bonferroni) = split /\t/, $line;
	  print OUT2 join("\t", $termId, $annotationName, $bgdCount, $resultCount, $pctOfBgd, $foldEnrichment, $oddsRatio, $pValue, $benjamini, $bonferroni) . "\n";
      }
  }
  close IN;
  close OUT2;
}

sub getTaxonId {
  my ($self, $dbh, $geneResultSql) = @_;

  my $sql = "
SELECT distinct ga.taxon_id
FROM  ApidbTuning.GeneAttributes ga,
     ($geneResultSql) r
where ga.source_id = r.source_id
";

  my $stmt = $self->runSql($dbh, $sql);
  my $count = 0;
  my $taxonId;
  while (my ($taxId) = $stmt->fetchrow_array()) { $taxonId = $taxId; $count++; }
  die "Result has genes from more than one taxon\n" if $count != 1;
  return $taxonId;
}

1;
