package ApiCommonWebsite::Model::WebXmlConfig;

use strict;
use Carp;
use XML::Simple;
use File::Basename;
use Data::Dumper;

sub new {
    my ($class, $webxml) = @_;
    my $self = {};
    bless $self;
       
    my $cfg = XMLin(
        $webxml ||= "$ENV{GUS_HOME}/config/web.xml",
        forcearray => ['context-parm', 'error-page'],
        KeyAttr => {'context-param' => 'param-name'},
    );
    
    for (keys %$cfg) {
        $self->{$_} = $cfg->{$_}
    }
    
    return $self;
}


# context params
sub getGusHome { $_[0]->{'context-param'}->{'GUS_HOME'}->{'param-value'} }
sub getWdkServiceEndpoint { $_[0]->{'context-param'}->{'WDK_SERVICE_ENDPOINT'}->{'param-value'} }

# attributes? RRD- this looks invalid but no time to explore
sub getModel { $_[0]->{'context-param'}->{'model'}->{'param-value'} }

1;
