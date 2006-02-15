package ApiCommonWebsite::View::CgiApp::MailProcessor;


use strict;
use CGI;
use Mail::Send;
use Mail::Sendmail;

my $MAIL_PROCESSOR_EMAIL = 'apache@pcbi.upenn.edu';

sub go {
    my $Self = shift;
    my $cgi = CGI->new();

    my $to1 = join("", @{ $cgi->{'to1'} });
    my $to2 = join("", @{ $cgi->{'to2'} });
    my $to = "$to1$to2";
    my $subject = join("", @{ $cgi->{'subject'} });
    my $replyTo = join("", @{ $cgi->{'replyTo'} }) || 'anonymous';
    my $privacy = join("", @{ $cgi->{'privacy'} });
    my $website = join("", @{ $cgi->{'website'} });
    my $version = join("", @{ $cgi->{'version'} });
    my $browser = join("", @{ $cgi->{'browser'} });
    my $message = join("", @{ $cgi->{'message'} });

    my $cfmMsg;

    # testing mode
    # $to = 'ygan@pcbi.upenn.edu';

    if ($to) {
      my $metaInfo = "Privacy preference: $privacy" . "\n"
	. "Website and version: $website $version" . "\n"
	. "Browser information: $browser";

      $cfmMsg = sendMail($to, $subject, $replyTo, $metaInfo, $message);

    } else {
      $cfmMsg .= ": no recipient is specified."
               . "\n\nThis indicate a problem with the mail form."
               . " Please contact the webmaster to report the problem.";
    }

    print $cgi->header('text/plain');
    print "$cfmMsg";
}

sub sendMail { return &_cpanMailSendmail(@_); }

sub _cpanMailSendmail {
    my ($to, $subject, $replyTo, $metaInfo, $message) = @_;

    my $fromName = $replyTo; $fromName =~ s/\@/\\\@/;

    my %mail = (From    => "$fromName <$MAIL_PROCESSOR_EMAIL>",
		To      => $to,
		Subject => $subject,
		'Reply-To'    => $replyTo,
		Message => "$metaInfo\n\n$message");

    my $success = sendmail(%mail);

    if (!$success) {
      return "Sorry your message was not sent: " . $Mail::Sendmail::error
	. "\n\nPlease use your browser's back button to go back and try again.";
    } else {
      return "Thank you! Your message has been sent."
	. "\n\nPlease use you browser's back button to go back to the website.";
    }
}

sub _cpanMailSend {
    my ($to, $subject, $replyTo, $metaInfo, $message) = @_;

    my $msg = new Mail::Send (Subject=>$subject,
			      To=>$to);
    # my $fh = $msg->open;               # some default mailer
    my $fh = $msg->open('sendmail'); # explicit
    print $fh "replyTo: $replyTo\n" . $metaInfo . "\n\n" . $message;
    $fh->close;         # complete the message and send it

    if ($?) {
      return "Sorry your message was not sent: $!"
	. "\n\nPlease use your browser's back button to go back and try again.";
    } else {
      return "Thank you! Your message has been sent."
	. "\n\nPlease use you browser's back button to go back to the website.";
    }
}

sub _unixMail {
    my ($to, $subject, $replyTo, $metaInfo, $message) = @_;
    my $body = "Reply-To: $replyTo" . "\n"
      . $metaInfo . "\n\n" . "$message" . "\n";

    system("echo '$body' | mail -s '$subject' $to");

    if ($?) {
      return "Sorry your message was not sent: $!"
	. "\n\nPlease use your browser's back button to go back and try again.";
    } else {
      return "Thank you! Your message has been sent."
	. "\n\nPlease use you browser's back button to go back to the website.";
    }
}

1;
