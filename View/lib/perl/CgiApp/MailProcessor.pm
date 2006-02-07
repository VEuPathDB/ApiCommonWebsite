package ApiCommonWebsite::View::CgiApp::MailProcessor;


use strict;
use CGI;

sub go {
    my $Self = shift;
    my $cgi = CGI->new();

    my $to = join("", @{ $cgi->{'to'} });
    my $subject = join("", @{ $cgi->{'subject'} });
    my $replyTo = join("", @{ $cgi->{'replyTo'} }) || 'anonymous';
    my $privacy = join("", @{ $cgi->{'privacy'} });
    my $website = join("", @{ $cgi->{'website'} });
    my $version = join("", @{ $cgi->{'version'} });
    my $browser = join("", @{ $cgi->{'browser'} });
    my $message = join("", @{ $cgi->{'message'} });

    my $cfmMsg = "Sorry your message was not sent";
    my $body;
    if ($to) {
      $body = "On behalf of: $replyTo" . "\n"
            . "Privacy preference: $privacy" . "\n"
            . "Website and version: $website $version" . "\n"
            . "Browser information: $browser" . "\n\n"
            . "$message" . "\n";

      system("echo '$body' | mail -s '$subject' $to");

      if ($?) {
	$cfmMsg .= ": $!"
                 . "\n\nPlease use your browser's back button to go back and try again.";
      } else {
	$cfmMsg = "Thank you! Your message has been sent."
                . "\n\nPlease use you browser's back button to go back to the website.";
      }
    } else {
      $cfmMsg .= ": no recipient is specified."
               . "\n\nThis indicate a problem with the mail form."
               . " Please contact the webmaster to report the problem.";
    }

    print $cgi->header('text/plain');
    print "$cfmMsg";

    if(0) {
      print "\n\nDEBUG: mailed to $to with the following message:\n$body";
    }
}

1;
