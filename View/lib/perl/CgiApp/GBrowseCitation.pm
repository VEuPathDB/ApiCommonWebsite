package ApiCommonWebsite::View::CgiApp::GBrowseCitation;
@ISA = qw( ApiCommonWebsite::View::CgiApp);

use ApiCommonWebsite::View::CgiApp;

use strict;

sub run {
  my ($self, $cgi) = @_;

  my $projectId = $cgi->param('project_id');
  my $tracks = $cgi->param('tracks');
  my @tracks = split(",", $tracks);

  print $cgi->header('text/html');
  print $cgi->start_html("$projectId:  GBrowse Citation(s)");

  print "<table>\n";

  my $parser = _CitationParser->new();

  foreach my $track (@tracks) {
    my $wgetCommand = "wget -qO- " . $cgi->url(-base => 1) . "/cgi-bin/gbrowse/" . lc($projectId) . "/?display_citation=$track";

    my $text = `$wgetCommand`;
    $parser->parse($text);
  }
  print "</table>\n";

  print $cgi->end_html;

}

1;

package _CitationParser;
use base "HTML::Parser";

use strict;

sub okToPrint { 
  my ($self, $v) = @_;

  if(defined($v)) {
    $self->{_ok_to_print} = $v;
  }
  $self->{_ok_to_print};
}


sub text {
  my($self, $tagname, $attr, $attrseq, $origtext) = @_;  

  if($self->okToPrint()) {
    print $tagname . "\n";
  }

}

sub start {
  my($self, $tagname, $attr, $attrseq, $origtext) = @_;  

  if($tagname eq 'tr') {
    $self->okToPrint(1);
  }

  if($self->okToPrint()) {
    print "<$tagname";

    foreach(keys %$attr) {
      print " $_=\"" . $attr->{$_} . "\"";
    }
    print ">\n";
  }
}

sub end {
  my($self, $tagname, $attr, $attrseq, $origtext) = @_;  

  if($self->okToPrint()) {
    print "</$tagname>\n";
  }

  if($tagname eq 'tr') {
    $self->okToPrint(0);
  }
}

1;
