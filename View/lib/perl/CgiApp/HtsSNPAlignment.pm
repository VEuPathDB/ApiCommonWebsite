package ApiCommonWebsite::View::CgiApp::HtsSNPAlignment;

@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

use CGI::Session;
use Bio::Graphics::Browser2::PadAlignment;
use Bio::SeqIO;
use Bio::Seq;

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
  my $p = $cgi->param('isolate_ids');
  $p =~ s/,$//;
  my @ids = split /,/, $p;
  my $list;
  foreach my $id (@ids){
    $id =~ s/ \(.+\)$//;
    $list = $list.  "'" . $id. "',";
  }
  $list =~ s/\,$//;

  $self->{ids} = $list;

}

sub handleIsolates {
  my ($self, $dbh, $cgi, $type) = @_;

  my $ids = $self->{ids};
  my $pan_names = $ids;

  my $start = $cgi->param('start');
  my $end   = $cgi->param('end');
  my $sid   = $cgi->param('sid');
  my $project_id = $cgi->param('project_id');
  my $organism   = $cgi->param('organism');
  my $metadata  = $cgi->param('metadata');

  $start =~ s/,//g;
  $end =~ s/,//g;

  if($end !~  /\d/) {
    $end   = $start + 50;
    $start = $start - 50;
  }

  my $sql;
  my $sth;

  # FOR displaying metadata
  if ($metadata) {
    my %data;
    $pan_names =~ s/'(\w*)'/'$1 (Sequence Variation)'/g;

    $sql = <<EOSQL;
SELECT REPLACE(pan_name,' (Sequence Variation)',''), term, value
FROM apidbtuning.pancharacteristicmetadata
WHERE subtype = 'HTS_SNP'
AND pan_name in ($pan_names)
AND organism = '$organism'
EOSQL
    $sth = $dbh->prepare($sql);
    $sth->execute();

    my ($tab,$newline)=("&nbsp;&nbsp;","<BR>");
    if ($type eq 'fasta'){
      ($tab,$newline)=("\t","\n");
    }

    while(my ($node, $term, $value) = $sth->fetchrow_array()) {
      $data{$node}->{$term} = $value;
    }

    print "### Metadata for the Strains: ### $tab $tab (Sequences are below)$newline";
    foreach my $key (sort keys %data) {
      print "#Strain=$key : ";
      foreach my $ca (sort keys  ($data{$key})) {
	print "$ca=". $data{$key}->{$ca} . "$tab" ;
      }
      print "$newline";
    }

    print "$newline$newline";
  }



# FOR Alignment/Fasta
  $ids =~ s/'(\w)/'$sid\.$1/g;
  $ids .= ",'$sid'";   # always compare with reference isolate

  $sql = <<EOSQL;
SELECT source_id, 
       substr(nas.sequence, $start,$end-$start+1) as sequence 
FROM   dots.nasequence nas
WHERE  nas.source_id in ($ids) 
EOSQL

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
    my @sequences;
    my %origins = ();

    while(my ($id, $seq) = $sth->fetchrow_array()) {
      my $noN = $seq;
      $noN =~ s/[ACGT]//g;
      next if length($noN) == length($seq);
      $id =~ s/^$sid\.// unless ($id eq $sid);
      push @sequences, ($id => $seq);
    }

    my @segments;
    my $align = Bio::Graphics::Browser2::PadAlignment->new(\@sequences,\@segments);

     foreach my $id (split /,/, $ids) {
        $id =~ s/'//g;
        $id =~ s/^$sid\.// unless ($id eq $sid);
        $origins{$id} = $start;
     }

  print "<table align=center width=800>";
  print "<tr><td>";
  print "<pre>";
  print $cgi->pre($align->alignment( \%origins, { show_mismatches   => 1,
						  show_similarities => 1, 
						  show_matches      => 1}));

  print "</pre>";
  print "</td></tr>";
  print "</table>";
  }
}

1;
