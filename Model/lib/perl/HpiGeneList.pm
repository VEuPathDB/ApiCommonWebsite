package ApiCommonWebsite::Model::HpiGeneList;

use strict;

use LWP::UserAgent;
use WDK::Model::ModelConfig;
use DBI;
use JSON;

use Data::Dumper;

sub new {
  my ($class)  = @_;

  my $self = {};
  bless( $self, $class );
  return $self;
}

sub run {
  my ($self, $idSql, $thresholdType, $threshold, $datasetCutoffType, $datasetCutoff, $useOrthology, $type, $idSource, $outputFile, $modelName, $server_endpoint) = @_;;

  print STDERR join("\n", @_) . "\n";
  
  my $ua = LWP::UserAgent->new;
  push @{ $ua->requests_redirectable }, 'POST';

  open(OUT, ">$outputFile") or die "Cannot open file $outputFile for writing: $!";

  my $c = new WDK::Model::ModelConfig($modelName);
  my $dbh = DBI->connect($c->getAppDb->getDbiDsn, $c->getAppDb->getLogin, $c->getAppDb->getPassword) or die DBI::errstr;

# get the valid gene IDs from the given input IDs
  my $geneList = &getValidGeneList($dbh, $idSql,$server_endpoint);

# set custom HTTP request header fields
  my $req = HTTP::Request->new(POST => $server_endpoint);
  $req->header('content-type' => 'application/json');

  ### vectorbase hack
  ### we may have to set up specific values for each specific endpoint so this may not in the end be a hack.
  $idSource = 'vectorbase' if $server_endpoint =~ /vectorbase/;

# add POST data to HTTP request body
  my $post_data = "{
    \"type\": \"$type\",
    \"idSource\": \"$idSource\",
    \"ids\": [ $geneList ],
    \"threshold\": \"$threshold\",
    \"thresholdType\": \"$thresholdType\",
    \"additionalFlags\": {
    \"useOrthology\": \"$useOrthology\",
    \"datasetCutoffType\": \"$datasetCutoffType\",
    \"datasetCutoff\": \"$datasetCutoff\"
    }
  }";


  print STDERR "POST=$post_data\n";

  $req->content($post_data);


  my $resp = $ua->request($req);
  if ($resp->is_success) {
    my $message = $resp->decoded_content;
    # print "Received reply: $message\n";

# experimentIdentifier,species,displayName,description,type,uri,t11,t12,t21,t22,significance
    # parse JSON string
    my $jsonString = decode_json($message);

    print STDERR Dumper ($jsonString);

    foreach my $d (@{$jsonString}) {
      my $desc = $d->{'description'};
      $desc =~s/[\t|\n]//g;

      my $significance = ($d->{'idLists'}[0])->{significance};
      my $c11 = ($d->{'idLists'}[0])->{c11};
      my $c22 = ($d->{'idLists'}[0])->{c22};
      my $c33 = ($d->{'idLists'}[0])->{c33};
      my $c44 = ($d->{'idLists'}[0])->{c44};
      my $c55 = ($d->{'idLists'}[0])->{c55};


#      my $significance = int(($d->{'idLists'}[0])->{significance} * 100000 + 0.5) / 100000;
      print OUT $d->{'experimentIdentifier'} . "\t" .
          $d->{'species'}  . "\t" .
          $d->{'displayName'}  . "\t" .
          $desc . "\t" .
          $d->{'type'}  . "\t" .
          $d->{'uri'}  . "\t" .
	  $c11 .  "\t" .
	  $c22 .  "\t" .
	  $c33 .  "\t" .
	  $c44 .  "\t" .
	  $c55 .  "\t" .
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
  my ($dbh, $sql,$server_endpoint) = @_;

  my @genes;
  ##vectorbase hack for demo
  if($server_endpoint =~ /vectorbase/){
    @genes = ('AAEL014955','AAEL014932','AAEL014943');
  }else{
    my $stmt = $dbh->prepare("$sql") or die(DBI::errstr);
    $stmt->execute() or die(DBI::errstr);
    
    
    my $geneStr;
    while ((my $mygene) = $stmt->fetchrow_array()) {
      $mygene =~ s/\.\d+$// if $server_endpoint =~ /patricbrc/; ##stripping off the version number
      push @genes, $mygene;
    }
  }

  die "Got no genes\n" unless scalar @genes > 1;

  return join(",", map { '"' . $_ . '"' } @genes);;
}


1;
