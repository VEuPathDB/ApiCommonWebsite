package ApiCommonWebsite::Model::HpiGeneList;

use strict;

use LWP::UserAgent;
use WDK::Model::ModelConfig;
use DBI;
use JSON;

sub new {
  my ($class)  = @_;

  my $self = {};
  bless( $self, $class );
  return $self;
}

sub run {
  my ($self, $idSql, $thresholdType, $threshold, $useOrthology, $type, $idSource, $outputFile, $modelName, $server_endpoint) = @_;;

  print STDERR join("\n", @_) . "\n";
  
  my $ua = LWP::UserAgent->new;
  push @{ $ua->requests_redirectable }, 'POST';

  open(OUT, ">$outputFile") or die "Cannot open file $outputFile for writing: $!";

  my $c = new WDK::Model::ModelConfig($modelName);
  my $dbh = DBI->connect($c->getAppDb->getDbiDsn, $c->getAppDb->getLogin, $c->getAppDb->getPassword) or die DBI::errstr;

# get the valid gene IDs from the given input IDs
  my $geneList = &getValidGeneList($dbh, $idSql);

# set custom HTTP request header fields
  my $req = HTTP::Request->new(POST => $server_endpoint);
  $req->header('content-type' => 'application/json');


# add POST data to HTTP request body
  my $post_data = "{
    \"type\": \"$type\",
    \"idSource\": \"$idSource\",
    \"ids\": [ $geneList ],
    \"threshold\": \"$threshold\",
    \"thresholdType\": \"$thresholdType\",
    \"additionalFlags\": {
    \"useOrthology\": \"$useOrthology\"
    }
  }";


  print STDERR "POST=$post_data\n";

  $req->content($post_data);


  my $resp = $ua->request($req);
  if ($resp->is_success) {
    my $message = $resp->decoded_content;
    # print "Received reply: $message\n";

# experimentIdentifier,species,displayName,description,type,uri,significance
    # parse JSON string
    my $jsonString = decode_json($message);

    foreach my $d (@{$jsonString}) {
      my $desc = $d->{'description'};
      $desc =~s/[\t|\n]//g;
      my $significance = sprintf("%0.2f", ($d->{'idLists'}[0])->{significance});
      print OUT $d->{'experimentIdentifier'} . "\t" .
          $d->{'species'}  . "\t" .
          $d->{'displayName'}  . "\t" .
          $desc . "\t" .
          $d->{'type'}  . "\t" .
          $d->{'uri'}  . "\t" .
	  $significance .  "\t" .
          ($d->{'idLists'}[0])->{uri}.   "\n";
    }
  } else {
    print "HTTP POST error code: ", $resp->code, "\n";
    print "HTTP POST error message: ", $resp->message, "\n";
  }

  close OUT;
  $dbh->disconnect();
}

sub getValidGeneList {
  my ($dbh, $sql) = @_;

  my $stmt = $dbh->prepare("$sql") or die(DBI::errstr);
  $stmt->execute() or die(DBI::errstr);

  my @genes;

  my $geneStr;
  while ((my $mygene) = $stmt->fetchrow_array()) {
    push @genes, $mygene;
  }

  die "Got no genes\n" unless scalar @genes > 1;

  return join(",", map { '"' . $_ . '"' } @genes);;
}


1;
