
package ApiCommonWebsite::View::CgiApp;

=pod

=head1 Purpose

Form a super-class for all cgi-bin webapps.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use CGI;

use ApiCommonWebsite::Model::ModelConfig;
use ApiCommonWebsite::Model::WebXmlConfig;

use DBI;
use DBD::Oracle;

# ========================================================================
# ------------------------------ Main Body -------------------------------
# ========================================================================

# --------------------------------- new ----------------------------------

sub new {
	 my $Class = shift;

	 my $Self = bless {}, $Class;

	 $Self->init(@_);

	 return $Self;
}

# --------------------------------- init ---------------------------------

sub init {
	 my $Self = shift;
	 my $Args = ref $_[0] ? shift : {@_};

   my $Cfg = ApiCommonWebsite::Model::WebXmlConfig->new(
                                                        "$ENV{GUS_HOME}/config/web.xml"
                                                       );

	 $Self->setModel           ( $Cfg->getModel );

	 return $Self;
}


# ------------------------------ Accessors -------------------------------

sub getModel           { $_[0]->{'Model'        } }
sub setModel           { $_[0]->{'Model'        } = $_[1]; $_[0] }

# ---------------------------------- go ----------------------------------

sub go {
	 my $Self = shift;

	 my $_cgi = $Self->cla();

	 $Self->run($_cgi);
}

# --------------------------------- cla ----------------------------------

sub cla {
	 my $Self = shift;

	 my $Rv   = CGI->new();

	 if (defined $Rv->param('help')) {
			usage();
			exit(0);
	 }

	 return $Rv;
}

# -------------------------------- usage ---------------------------------

sub usage {
	 my $Self = shift;

	 my $class_f = $INC{ref($Self). '.pm'};

	 system "pod2text $0 $class_f";
}

# ---------------------------- getQueryHandle ----------------------------

sub getQueryHandle {
   my $Self = shift;

   my $Rv;

   my $_config = new ApiCommonWebsite::Model::ModelConfig($Self->getModel());
                       
   $Rv = DBI->connect( $_config->getDbiDsn(),
                       $_config->getLogin(),
                       $_config->getPassword()
                     )
   || die "unable to open db handle to ", $_config->getDbiDsn();

   # solve oracle clob problem; not that we're liable to need it...
   $Rv->{LongTruncOk} = 0;
   $Rv->{LongReadLen} = 10000000;

   return $Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;

