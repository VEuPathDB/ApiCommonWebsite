package ApiCommonWebsite::View::CgiApp::SpamCan;

use strict;
use HTML::Parser;
use ApiCommonWebsite::View::CgiApp::Akismet;

#
# Register rule names and corresponding code references that
# return 1 if spam rule matches.
#
# If any rule matches, the message is flagged as spam.
#
my %RULES = (
  Hello => \&hello,
  Akismet => \&akismet,
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


# Akismet is an online service that evaluates blog comment postings. 
# http://akismet.com/
sub akismet {
    my ($replyTo, $subject, $message, $ipaddr, $browser) = @_;

    my $akismet = ApiCommonWebsite::View::CgiApp::Akismet->new(
        KEY => 'ed563771dff9',
        URL => 'http://apidb.org/',
    );
    
    (warn "Unable to establish Akismet check" and return 0) unless $akismet; # skip rule if Akismet's servers are down

    my $answer = $akismet->check(
        USER_IP                 => $ipaddr,
        COMMENT_USER_AGENT      => $browser,
        COMMENT_CONTENT         => $message,
        COMMENT_AUTHOR_EMAIL    => $replyTo,
    );
    
    my $warn = <<"EOF";
No answer from Akismet
        USER_IP                 => $ipaddr,
        COMMENT_USER_AGENT      => $browser,
        COMMENT_CONTENT         => $message,
        COMMENT_AUTHOR_EMAIL    => $replyTo,

EOF
    (warn $warn and return 0) unless $answer;
    
    warn "akismet, is spam? $answer";
    warn "$replyTo, $subject, $message, $ipaddr";
    
    return ($answer eq 'true') ? 1 : 0;

}




################################################################
# general subroutines
################################################################

sub tastesLikeSpam {
  my ($self, $replyTo, $subject, $message, $ipaddr, $browser) = @_;

  map { 
    return 1 if &{$RULES{$_}}($replyTo, $subject, $message, $ipaddr, $browser);
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
