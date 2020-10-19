package ApiCommonWebsite::View::CgiApp::HtsSNPAlignment;

@ISA = qw( EbrcWebsiteCommon::View::CgiApp );

use strict;
use EbrcWebsiteCommon::View::CgiApp;

use CGI::Session;
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

#  my $ids = $self->{ids};
#  my $pan_names = $ids;

  my $filterPredicates = $self->{filter_predicates};
  my $filterPredicatesCount = scalar @$filterPredicates;

  my $filterPredicatesString = $filterPredicatesCount ? "AND (" . join(" OR ", @$filterPredicates) . ")" : "";
  my $filterPredicatesCountString = $filterPredicatesCount ? "and ct.n = $filterPredicatesCount" : "";

  my $start = $cgi->param('start');
  my $end   = $cgi->param('end');
  my $sid   = $cgi->param('sid');
  my $project_id = $cgi->param('project_id');
  my $organism   = $cgi->param('organism');
  my $metadata  = $cgi->param('metadata');

  $start =~ s/,//g;
  $end =~ s/,//g;

  my $sql;
  my $sth;

  # FOR displaying metadata

  my %data;
#    $pan_names =~ s/'(\w*)'/'$1 (Sequence Variation)'/g;

    $sql = <<EOSQL;
with m as
(SELECT pan_id, REPLACE(pan_name,' (Sequence Variation)','') pan_name, property, string_value
FROM apidbtuning.metadata
WHERE dataset_subtype = 'HTS_SNP'
AND organism = '$organism'
$filterPredicatesString
)
select m.*
from m, (select pan_id, count(*) n from m group by pan_id) ct
where ct.pan_id = m.pan_id 
$filterPredicatesCountString
EOSQL
	
	#print Dumper $sql; 
    $sth = $dbh->prepare($sql);
    $sth->execute();

    my ($tab,$newline)=("&nbsp;&nbsp;","<BR>");
    if ($type eq 'fasta'){
      ($tab,$newline)=("\t","\n");
    }

    while(my ($panId, $node, $term, $value) = $sth->fetchrow_array()) {
      $data{$node}->{$term} = $value;
    }

	#print Dumper %data; 

  if ($metadata) {

    print "### Metadata for the Strains: $newline"; # $tab $tab (Sequences are below)$newline";
    foreach my $key (sort keys %data) {
      print "#Isolate=$key :  ";
      foreach my $ca (sort keys  ($data{$key})) {
	print "$ca= ". $data{$key}->{$ca} . "$tab" ;
      }
      print "$newline";
    }
    print "$newline$newline";
  }

  my $ids = join(",", map { "'$_'" } keys %data);

# FOR Alignment/Fasta
  $ids =~ s/'(\w)/'$sid\.$1/g;
  $ids .= ",'$sid'";   # always compare with reference isolate

  $sql = <<EOSQL;
SELECT source_id, 
       substr(nas.sequence, $start,$end-$start+1) as sequence 
FROM   dots.nasequence nas
WHERE  nas.source_id in ($ids) 
EOSQL


  #print STDERR Dumper $sql; 
  $sth = $dbh->prepare($sql);
  $sth->execute();

  if ($type eq 'fasta'){
    my %seqH;
    while(my ($id, $seq) = $sth->fetchrow_array()) {
      my $noN = $seq;
      $noN =~ s/[ACGT]//g;
      next if length($noN) == length($seq);
      $id =~ s/^$sid\.// unless ($id eq $sid);
      $seqH{$id}= $seq;
    }

    my $seqIO = Bio::SeqIO->new(-fh => \*STDOUT, -format => 'fasta');

    foreach my $id (keys %seqH){
      my $sequence = $seqH{$id};
      $sequence =~ s/-//g;

      my $fastaSeq = Bio::Seq->new( -seq => $sequence,
                                    -display_id  => $id
                                     );
      $seqIO->write_seq($fastaSeq);
    }
    $seqIO->close();
  }


  if ($type =~ /htsSNP/i){
    my $sequence;
    my %origins = ();

    while(my ($id, $seq) = $sth->fetchrow_array()) {
      my $noN = $seq;
      $noN =~ s/[ACGT]//g;
      next if length($noN) == length($seq);
      $id =~ s/^$sid\.// unless ($id eq $sid);
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

  ## Interacting with iTOL to make a tree.
  ## NOTE - check elsewhere this is used when done. SNP etc.
  ## This uses the dnd file out put.

  my $ua = LWP::UserAgent->new;
  my $request = HTTP::Request::Common::POST( 'https://itol.embl.de/upload.cgi',
     Content_Type => 'form-data',
     Content      => [
                      # ttext => $dndData,
                     ]);
  my $response = $ua->request($request);
  my $iTOLLink =  "https://itol.embl.de/" . $response->{'_headers'}->{'location'};
  my $iTOLHTML = "";
  &createHTML($iTOLHTML,$outfile,$cgi,%origins);
  }
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

1;
