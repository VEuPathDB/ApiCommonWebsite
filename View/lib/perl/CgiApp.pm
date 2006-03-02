
package PlasmoDBWebsite::View::CgiApp;

=pod

=head1 Purpose

Form a super-class for all cgi-bin webapps.

=cut

# ========================================================================
# ----------------------------- Declarations -----------------------------
# ========================================================================

use strict;

use CGI;

use CBIL::Util::Disp;
use CBIL::Util::Configuration;

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

	 $Self->setConfigFile           ( $Args->{ConfigFile          } );

	 return $Self;
}


# ------------------------------ Accessors -------------------------------

sub getConfigFile           { $_[0]->{'ConfigFile'        } }
sub setConfigFile           { $_[0]->{'ConfigFile'        } = $_[1]; $_[0] }

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

	 if (not defined scalar($Rv->param) ||
			 defined $Rv->param('help')
			) {
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

   my $_config = CBIL::Util::Configuration->new({ ConfigFile => $Self->getConfigFile(),
                                                  Delimiter  => '\s*=\s*',
                                                });

   $Rv = DBI->connect( $_config->{dbiDsn},
                       $_config->{databaseLogin},
                       $_config->{databasePassword}
                     )
   || die "unable to open db handle";

   # solve oracle clob problem; not that we're liable to need it...
   $Rv->{LongTruncOk} = 0;
   $Rv->{LongReadLen} = 10000000;

   return $Rv;
}

# ========================================================================
# ---------------------------- End of Package ----------------------------
# ========================================================================

1;

__END__

# A heavier weight way to do this...

use GUS::Supported::GusConfig;
use GUS::ObjRelP::DbiDatabase;

sub getQueryHandle {
	 my $Self = shift;
	 my $Rv;

	 my $gusconfig = GUS::Supported::GusConfig->new($Self->getConfigFile());

	 my $db = GUS::ObjRelP::DbiDatabase->new($gusconfig->getDbiDsn(),
						 $gusconfig->getReadOnlyDatabaseLogin(),
						 $gusconfig->getReadOnlyDatabasePassword,
						 0,0,1,
						 $gusconfig->getCoreSchemaName(),
						 $gusconfig->getOracleDefaultRollbackSegment()
						);
	 $Rv = $db->getQueryHandle();

	 return $Rv;
}

