package ApiCommonWebsite::Model::DbUtils;
use Exporter 'import';
@EXPORT_OK = qw(
    resolveOracleDSN
    resolveOracleAlias
    jdbc2oracleDbi
    dbi2connectString
    );

=head1 NAME

ApiCommonWebsite::Model::DbUtils - utility methods for database 
connection configuration.

=cut

use strict;
use DBI 1.43;

=head1 METHODS


=head2 resolveOracleDSN

 Usage : my $newDsn = DbUtils->resolveOracleDSN($dsn);
 Returns : DSN with the Oracle alias resolved by the 
 Oracle utility 'tnsping'

 Expand the Oracle DSN by resolving the tnsname.
 Useful for working around issue where Apache segfaults or 
 other errors occur when letting DBD::Oracle do tnsname resolution  
 via LDAP in Mod::Registry scripts. The Apache segfaults are
 avoided if the name is pre-resolved before handing it to DBD::Oracle.

=over 4

 my $newDsn = DbUtils->resolveOracleDSN('dbi:Oracle:cryptoA');
 print $newDsn;
 dbi:Oracle():(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=ahost.xyz.uga.edu)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=ahost.xyz.uga.edu)))

=cut
sub resolveOracleDSN {
    my ($class, $dsn) = @_;
    my ($scheme, $driver, $attr_string, $attr_hash, $driver_dsn) = DBI->parse_dsn($dsn);
    my $tnsname = $class->resolveOracleAlias($driver_dsn);
    return   ( ($scheme) && "$scheme:" ) .
             ( ($driver) && "$driver"  ) .
             ( ($attr_string) ? "($attr_string):" : ':'  ) .
             $tnsname;
}

=head2 resolveOracleAlias

 Usage : my $name = DbUtils->resolveOracleAlias($alias);
 Returns : the Oracle alias as resolved by the Oracle utility 
 'tnsping'
 
 See resolveOracleDSN() for possible use case.

 my $name = DbUtils->resolveOracleDSN('cryptoA');
 print $name;
 (DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=ahost.xyz.uga.edu)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=ahost.xyz.uga.edu)))

=cut
sub resolveOracleAlias {
    my ($class, $alias) = @_;
    return qx{ 
        $ENV{ORACLE_HOME}/bin/tnsping '$alias' | \
        grep 'Attempting to contact' | \
        sed 's/Attempting to contact //'
    };
}

=head2 jdbc2oracleDbi

 Usage : my $name = DbUtils->jdbc2oracleDbi($jdbcConnectUrl);
 Returns : dbi syntax converted from Oracle thin jdbc driver syntax

 my $name = DbUtils->jdbc2oracleDbi('jdbc:oracle:thin:@redux.rcc.uga.edu:1521:cryptoB');
 print $name;
 dbi:Oracle:@redux.rcc.uga.edu:1521:cryptoB

=cut
sub jdbc2oracleDbi {
    my ($jdbc) = @_;
    
    if ($jdbc =~ m/thin:[^@]*@([^:]+):([^:]+):([^:]+)/) {
        # jdbc:oracle:thin:@redux.rcc.uga.edu:1521:cryptoB
        my ($host, $port, $sid) = $jdbc =~ m/thin:[^@]*@([^:]+):([^:]+):([^:]+)/;
        return "dbi:Oracle:host=$host;sid=$sid;port=$port";
    } elsif ($jdbc =~ m/@\(DESCRIPTION/i) {    
        # jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=redux.rcc.uga.edu)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=cryptoB.rcc.uga.edu)))
        my ($tns) = $jdbc =~ m/[^@]+@(.+)/;
        return "dbi:Oracle:$tns";
    } elsif ($jdbc =~ m/:oci:@/) {
       # jdbc:oracle:oci:@toxoprod
       my ($sid) = $jdbc =~ m/:oci:@(.+)/;
        return "dbi:Oracle:$sid";
    } else {
        # last ditch effort.
        # jdbc:oracle:thin:@kiwi.rcr.uga.edu/cryptoB.kiwi.rcr.uga.edu
        $jdbc =~ m/thin:[^@]*@(.+)/;
        return "dbi:Oracle:$1";
    }
}

1;
