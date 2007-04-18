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


sub getModelConfigBasename { basename($_[0]->getWdkModelConfigXml()) }

sub getModelName { scalar fileparse($_[0]->getWdkModelXml(), '.xml') }

# context params
sub getWdkCustomViewDir { $_[0]->{'context-param'}->{'wdkCustomViewDir_param'}->{'param-value'} }
sub getWdkModelSchema { $_[0]->{'context-param'}->{'wdkModelSchema_param'}->{'param-value'} }
sub getWdkModelProps { $_[0]->{'context-param'}->{'wdkModelProps_param'}->{'param-value'} }
sub getWdkModelConfigXml { $_[0]->{'context-param'}->{'wdkModelConfigXml_param'}->{'param-value'} }
sub getWdkModelParser { $_[0]->{'context-param'}->{'wdkModelParser_param'}->{'param-value'} }
sub getWdkXmlDataDir { $_[0]->{'context-param'}->{'wdkXmlDataDir_param'}->{'param-value'} }
sub getWdkXmlSchema { $_[0]->{'context-param'}->{'wdkXmlSchema_param'}->{'param-value'} }
sub getWdkAlwaysGoToSummary { $_[0]->{'context-param'}->{'wdkAlwaysGoToSummary_param'}->{'param-value'} }
sub getWdkModelXml { $_[0]->{'context-param'}->{'wdkModelXml_param'}->{'param-value'} }


1;