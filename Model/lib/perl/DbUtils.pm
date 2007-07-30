package ApiCommonWebsite::Model::DbUtils;

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
 
=over 4

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



1;
