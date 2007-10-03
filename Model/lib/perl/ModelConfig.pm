package ApiCommonWebsite::Model::ModelConfig;

use strict;
use XML::Simple;

sub new {
    my ($class, $model) = @_;
    my $self = {};
    bless $self;
   
    my $cfg = XMLin("$ENV{GUS_HOME}/config/${model}/model-config.xml");
    
    for (keys %$cfg) {
        $self->{$_} = $cfg->{$_}
    }
    
    $self->{dbiDsn} = _jdbc2dbi($self->{connectionUrl});

    return $self;
}

sub getDbiDsn             { $_[0]->{dbiDsn} }
sub getLogin              { $_[0]->{login} }
sub getPassword           { $_[0]->{password} }
sub getConnectionUrl      { $_[0]->{connectionUrl} }
sub getQueryInstanceTable { $_[0]->{queryInstanceTable} }
sub getQueryHistoryTable  { $_[0]->{historyTable} }
sub getPlatformClass      { $_[0]->{platformClass} }
sub getMaxQueryParams     { $_[0]->{maxQueryParams} }
sub getMaxIdle            { $_[0]->{maxIdle} }
sub getMaxWait            { $_[0]->{maxWait} }
sub getMaxActive          { $_[0]->{maxActive} }
sub getMinIdle            { $_[0]->{minIdle} }
sub getInitialSize        { $_[0]->{initialSize} }
sub getWebServiceUrl      { $_[0]->{webServiceUrl} }

sub _jdbc2dbi {
# convert Oracle thin jdbc driver syntax to dbi syntax

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

__END__

=head1 NAME

ApiCommonWebsite::Model::ModelConfig - access to WDK model-config.xml properties

=head1 SYNOPSIS

    use ApiCommonWebsite::Model::ModelConfig;

    my $cfg = new ApiCommonWebsite::Model::ModelConfig('TrichDB');
    
    my $username = $cfg->getLogin;
    my $password = $cfg->getPassword;

    Retrieve the JDBC connectionUrl converted to Perl DBI syntax:
    my $dsn = $cfg->getDbiDsn;
    

    You may also access by property name:
        $cfg->{login}
    but be aware that if the property name changes in the model-config.xml you
    will need to update every script that uses this accessor.
    
    $cfg->{connectionUrl} is the JDBC connection string as written in the 
    model-config.xml.
    $cfg->{dbiDsn} is the Perl DBI version translated from the 
    connectionUrl property.
    
=head1 DESCRIPTION

Provides Perl access to properties in a WDK model-config.xml file.

=head1 BUGS

The conversion of the JDBC connectionUrl to Perl DBI only works for Oracle
thin driver syntax, and even then not for all allowed syntax.
Assumes connection strings of the format
  jdbc:oracle:thin:@hostname.uga.edu:1521:trichsite

=head1 AUTHOR 

Mark Heiges, mheiges@uga.edu

=cut

=head1 METHODS

=head2 new

 Usage   : my $cfg = new ApiCommonWebsite::Model::ModelConfig('TrichDB');
 Returns : object containing data parsed from the model configuration file.
 Args    : the name of the model. This follows the name convention used for
           the WDK commandline utilities. This is used to find the Model's 
           configuration XML file ($GUS_HOME/config/{model}/model-config.xml)

=head2 getLogin
 
 Usage : my $username = $cfg->getLogin;
 Returns : login name for the database
 
=head2 getPassword
 
 Usage : my $passwd = $cfg->getPassword;
 Returns : login password for the database
 
=head2 getDbiDsn
 
 Usage : my $dsn = $cfg->getDbiDsn;
 Returns : perl dbi connection string. converted from the jdbc connection URL in the model-config.xml
 Example : dbi:Oracle:host=redux.rcc.uga.edu;sid=trichsite
 
=head2 getConnectionUrl
 
 Usage : my $jdbcUrl = $cfg->getConnectionUrl;
 Returns : original jdbc connection string from model-config.xml



=head2 getQueryInstanceTable

=head2 getQueryHistoryTable 

=head2 getPlatformClass     

=head2 getMaxQueryParams    

=head2 getMaxIdle           

=head2 getMaxWait           

=head2 getMaxActive         

=head2 getMinIdle           

=head2 getInitialSize       

=head2 getWebServiceUrl     

 
=cut

