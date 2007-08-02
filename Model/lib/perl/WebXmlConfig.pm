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
sub getModel { $_[0]->{'context-param'}->{'model'}->{'param-value'} }
sub getWdkAlwaysGoToSummary { $_[0]->{'context-param'}->{'wdkAlwaysGoToSummary_param'}->{'param-value'} }
sub getWdkCustomViewDir { $_[0]->{'context-param'}->{'wdkCustomViewDir_param'}->{'param-value'} }
sub getWsfConfigDir { $_[0]->{'context-param'}->{'wsfConfigDir_param'}->{'param-value'} }

1;
