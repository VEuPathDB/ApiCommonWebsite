# Test module for CgiApp
# returns SYSDATE from dual and reports if 
# running in  cgi or Apache::Registry environment

package ApiCommonWebsite::View::CgiApp::SimpleDbQuery;
@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

sub run {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);

  print $cgi->header();

  my $sql = 'select sysdate from dual';
  
  my $sth = $dbh->prepare($sql);
  $sth->execute();
      
  while (my @row = $sth->fetchrow_array) {
      print $row[0] . "<br>\n";
  }
  
  $sth->finish();
  
  chomp(my $this = `basename $0`);
  print "<p>$this running under @{[ 
      ($ENV{MOD_PERL}) ? 
          'mod_perl' :
          'cgi'
  ]}";
}

1;
