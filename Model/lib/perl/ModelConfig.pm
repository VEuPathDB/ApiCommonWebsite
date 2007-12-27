package ApiCommonWebsite::Model::ModelConfig;

use strict;
use XML::Simple;
use ApiCommonWebsite::Model::DbUtils qw(jdbc2oracleDbi dbi2connectString);

sub new {
    my ($class, $model) = @_;
    my $self = {};
    bless $self;
   
    my $modelconfig = "$ENV{GUS_HOME}/config/${model}/model-config.xml";
    (-e $modelconfig) or die "File not found: $modelconfig\n";

    my $cfg = XMLin($modelconfig);
    
    for (keys %$cfg) {
        $self->{$_} = $cfg->{$_}
    }
    
    $self->{dbiDsn} = jdbc2oracleDbi($self->{connectionUrl});

    ($self->{connectString} = $self->{dbiDsn}) =~ s/dbi:Oracle://;

    return $self;
}

sub AUTOLOAD {
    my $attr = our $AUTOLOAD;
    $attr =~ s/.*:://;
    $attr =~ s/get([A-Z])/$1/;
    $attr = lcfirst($attr);
    $_[0]->{ $attr } || die "`$attr' not defined.";
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
        $cfg->login
    
    $cfg->connectionUrl is the JDBC connection string as written in the 
    model-config.xml.
    $cfg->dbiDsn is the Perl DBI version translated from the 
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

=head2 getConnectString
 
 Usage : my $connect = $cfg->getConnectString;
 Returns : connect string suitable for non-DBI cases (e.g. sqlplus)



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

