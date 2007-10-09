package ApiCommonWebsite::View::CgiApp::SpamCan;

use strict;
use HTML::Parser;

#
# Register rule names and corresponding code references that
# return 1 if spam rule matches.
#
# If any rule matches, the message is flagged as spam.
#
my %RULES = (
  Hello => \&hello,
);



################################################################
# Rules - return 1 (true) if rule matches, 0 (false) otherwise
################################################################

# Our most common spam to date. An empty email address (defaulting
# to 'anonymous') and lots of links. Subject is always 'Hello'.
sub hello {
  my ($replyTo, $subject, $message) = @_;

  my $link_limit = 10;

  return ( 
    ($replyTo =~ m/anonymous/i || $replyTo =~ m/^\s*$/) && 
    _linkCount($message) > $link_limit && 
    $subject =~ m/^\s*hello\s*$/i
  );
}












################################################################
# general subroutines
################################################################

sub tastesLikeSpam {
  my ($self, $replyTo, $subject, $message) = @_;
  
  map { 
    return 1 if &{$RULES{$_}}($replyTo, $subject, $message);
  } keys %RULES;

  return 0;
}


sub _linkCount {
  my ($html) = @_;
  my $links = 0;
  
  my $parser = HTML::Parser->new(start_h => [
    sub { $links++ if $_[0] eq 'a'; },
    'tagname']);
    
  $parser->parse( $html );
  
  return $links;
}




sub new {
    my ($class) = @_;
    my $self = {};
    bless $self, $class;
    return $self;
}



1;