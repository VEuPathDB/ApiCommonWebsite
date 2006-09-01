#!/usr/bin/perl -w

# current: check to work forms by putting link into action ???
# to do:
# - change all search fields to the short qualifier tag before sending of a query!
# - selecting a different output format for GenBank records doesn't work from the results page
# - user 'dpchoma': hash in alias name!

use strict;

my $gnugpl = "

#    PubCrawler - a free update alerting service
#    Copyright (C) 1999 - 2004 K.H. Wolfe, K. Hokamp
#
#  This program is free software; you can redistribute it and/or
#  modify it under the terms of the GNU General Public License
#  as published by the Free Software Foundation; either version 2
#  of the License, or (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
#
#    For more information on PubCrawler
#    see http://www.pubcrawler.ie
#    or POD-text at end of file

";

# Version number is automatically assigned
# by RCS (revision control system):
my $version_number = '$Revision: 12946 $'; # '  $Date: 2006-08-24 12:37:28 -0400 (Thu, 24 Aug 2006) $
$version_number =~ s/[\$Revision\: | \$]//g;

# LOCATION OF MODULES:
# In case you have Perl modules installed
# in one of your own directories,
# edit the following line by adding any paths
# in which PERL should look for modules...
BEGIN {
    unshift(@INC, "$ENV{HOME}/lib", "$ENV{HOME}/PubCrawler/lib",
              '/full/path/to/module/directory', './lib');
      # there are three example paths provided:
      # - the first one ("$ENV{HOME}/lib") points to the directory
      #   'lib' below your HOME-directory (~/lib or $HOME/lib);
      #   this works only if the environmental variable $HOME is set;
      # - the second one follows the same syntax as the first
      # - the third path ('/path/to/module') is an example for any
      #   kind of absolute UNIX-path starting from the root ('/')

      # ADD /current_working_directory/lib to @INC
      # (activated with option -add_path)
    if (grep /\Q-add_path/, @ARGV) {
    my $tmp_file = "/tmp/pubcrawler_pwd.$$";
    system "pwd > $tmp_file";
    open (IN, "$tmp_file");
    chomp(my $cwd = <IN>);
    close IN;
    unlink $tmp_file;
    push @INC, "$cwd/lib";
    print STDERR "\nTemporarily added path $cwd/lib to \@INC\n";
    }

      # LIBRARY TEST:
      # (activated with option -lib_test)
    if (grep /\Q-lib_test/, @ARGV) {
    print STDERR "\n          ***** PubCrawler - library test *****\n";
    print STDERR "\nThe following directories will be searched for modules:\n\n";
    foreach (@INC) {
        print STDERR "$_\n";}
    exit;
    }
}               

#### STANDARD MODULES ####
use Getopt::Long;    # to read in command line options
use File::Basename;  # to parse file- and path-names
use File::Copy;      # to move files
use Cwd;             # to get the current working directory
use Fcntl;           # for unbuffered writing to results file

#### ADDITIONAL MODULES ####
use LWP::Simple;     # to retrieve proxy autoconfig-file
use HTML::Parser;    # to parse HTML-expressions
use LWP::UserAgent;  # for advanced internet connections

###############################################################
####################  PROGRAM VARIABLES  ######################
###############################################################

$| = 1;  # print to STDOUT immediately

#$mail_prog = '/usr/bin/sendmail -t -n -oi';  # program used for e-mail
# I'm using metasend, because it allows me
#  - to send the results as an HTML-file
#  - to set the From: header manually
# drawback is, that I have to store the message
# in a file locally
# /usr/bin/mail would be easier to use
# but is quite limited...
my $sender = 'pubcrawler@tcd.ie';     # <- PLEASE change to YOUR address
my $splitsize = '3000000';  # for metasend: don't split mails smaller than that
my $mail_prog = "/usr/bin/metasend -b -F $sender -S $splitsize";
my $sendmail = '/usr/sbin/sendmail'; # for text messages that don't need to be attached

#my $warn_stat = $^W;  # store status of warning-switch

my $cwd = cwd;        # get current working directory

# I am just guessing here, that the following operating systems
# will work alright with the 'system'-variable set to 'unix'
my @unix_flav = qw( aix dec_os dec_osf dynix epix esix freebsd genix hpux
        irix isc linux lynxos machten mips mpc mpeix
        netbsd nwsos next openbsd powerux qnx sco solaris
        stellar sunos svr4 ti1500 titanos ultrix umips
        unicos unisys unix utek uts cygwin );
my $unix_flav = join '|', @unix_flav;

my $link_gen = 'http://pubcrawler.gen.tcd.ie';
my $home_link = 'http://www.pubcrawler.ie';

my %db_match = ('pubmed', 'm',     # matches database key
        'genbank', 'n',    # to query-option
        'nucleotide', 'n',    # to query-option
        'pm_neighbour', 'relm',    # to query-option
        'gb_neighbour', 'reln'
        );
my %back_match = ('pm_neighbour', 'pubmed',
          'gb_neighbour', 'nucleotide'
          );
my %search_db = ('m', 'pubmed',
         'n', 'nucleotide'
         );
my %date_limit = ('m','reldate',
          'n','reldate'
          );
my %date_range = ('m','entrezdate',
          'n','moddate'
          );
                                              
my %word = ('pubmed', 'PubMed',    #matches database key
     'nucleotide', 'GenBank',  #to the real word 
     'genbank', 'GenBank', 
     'pm_neighbour', 'Medline Neighbourhood',  #to the real word 
     'gb_neighbour', 'Nucleotide Neighbourhood'
     );  
my %retrieve_db = ('pubmed', 'PubMed',         #matches database key
           'nucleotide', 'nucleotide', #to databases recognized for retrieval links        
           );  

my %ncbi_options = ('pubmed', 'DocSum, Brief, Abstract, Citation, MEDLINE, XML, ASN1, ExternalLink',
            'nucleotide', 'DocSum, Brief, GenBank, ASN1, FASTA, ExternalLink, XML');
my %ncbi_options_string = ();
foreach (keys %ncbi_options) {
    $ncbi_options_string{$_} = '"'.(join '", "', (split /, /, $ncbi_options{$_})).'"';
}

    # print buttons for retrieval of records
    my $ncbi_buttons = "
This feature requires Javascript to be activated in your browser.
<BR>
<BR>
<input name=\"allbox\" type=\"checkbox\" value=\"Check All\" onClick=\"CheckAll();\">(un)select all boxes
<INPUT TYPE=\"button\"
     VALUE=\"toggle\"
     ONCLICK=\"ToggleAll(this.form);\">
<BR>
<TABLE>
<TR>
<TD>
Select database:
</TD>
<TD>
<SELECT NAME=\"NCBIDB\" onChange=\"setFormat(this)\">
<OPTION VALUE=\"pubmed\" SELECTED>PubMed
<OPTION VALUE=\"nucleotide\">Nucleotide
</SELECT>
</TD>
<TD ROWSPAN=2 VALIGN=MIDDLE>
<INPUT TYPE=\"button\"
     VALUE=\"Retrieve!\"
     ONCLICK=\"id_collect_ncbi(this.form);\">
</TD>
</TR>
<TR>
<TD>
Select format:
</TD>
<TD>
<SELECT NAME=\"NCBIFormat\">
";

   my $ncbi_options = $ncbi_options{'pubmed'};
   my $retrieve_format = 'Abstract';
   #$ncbi_options =~ s/\s+//g;
   my @ncbi_options = split /,\s*/, $ncbi_options;
   foreach (@ncbi_options) {
       my $opt = lc $_;
       $ncbi_buttons .= "<OPTION VALUE=\"$opt\"".
       ( ($_ eq $retrieve_format
          or
          $opt eq $retrieve_format) ?
         ' SELECTED' : '').
         ">$_</OPTION>\n";
   }
#    $ncbi_buttons .= "
#</SELECT>
#</TD>
#</TR>
#<TR>
#<TD COLSPAN=3><CENTER>(Defaults for this menu can be stored in your profile.)</CENTER>
#</TD>
#</TR>
#</TABLE>
#</FORM>
#<form method=\"post\" action=\"\" name=\"gotoNCBI\"></form>
#<BR>
#";

    $ncbi_buttons .= "
</SELECT>
</TD>
</TR>
</TABLE>
<BR>
";

                                                    
my $known_searchtypes = '(pubmed|genbank|nucleotide|pm_neighbour|gb_neighbour)';   # all databases that we allow

my $EXIT_SUCCESS = 0; # return value for successful exit
my $EXIT_FAILURE = 1; # return value for unsuccessful exit

my $indent = 0;
my $prev_indent = '';

# parse program name
my ($prog_name,$program,$suffix);
($prog_name,undef,$suffix) = fileparse("$0",'\..*');
$program = $prog_name.$suffix;

# help-message:
my $USAGE = "
          ***** PubCrawler $version_number - help message *****

usage: $program [-add_path] [-c <config_file>]
       [-check] [-copyright] [-d <directory>] [-db <database>]
       [-extra_range <range for extra entries>] [-force_mail] 
       [-format <results format>] [-fullmax <max-docs in full>]
       [-getmax <max-docs to get>] [-h] [-help] [-head <output-header>]
       [-i] [-indent <pixels>] [-l <log_file>] 
       [-lynx <alternative-browser>] [-mail <address for results]   
       [-mail_ascii <address for text-only results]
       [-mail_simple <address for slim HTML results] [-mute] 
       [-n <neighbour_URL>] [-notify <address for notification]
       [-no_test] [-os <operating_system>]
       [-out <output-file>] [-p <proxy_server>] [-pp <proxy_port>]
       [-pauth <proxy_authorization>] [-ppass <proxy_password>]
       [-pre <prefix>] [-q <query_URL>] [-r <retrieve_URL>] 
       [-relentrezdate <relative-entrez-date>] [-retry <number of retries>]
       [-s <search-term] [-spacer <gif>] [-t <timeout>] [-u <test_URL>] 
       [-v <verbose>] [-viewdays <view-days>] [-version]

options:
-add_path adds the path /cwd/lib to \@INC (list of library directories)
          where cwd stands for the current working directory
-c       configuration file for pubcrawler
-check   checks if program and additional files are setup correctly
-copyright shows copyright information
-d       pubcrawler directory (configuration,databases,and output)
-db      name of database file
-extra_query specifies additional info that is sent out with each query
-extra_range specifies the number of documents combined in a link
             minimum value is 1, defaults to 'fullmax'
-force_mail forces mailing of results/note even if 0 hits
-format  format of reports (defaults to DocSum)
-fullmax maximum number of full length reports shown (per search)
-getmax  maximum number of documents to retrieve (per search)
-h       this help message
-head    HTML-header for output file
-help    same as -h
-i       include configuration file in HTML-output
-indent  indent PubCrawler comments n pixel (default 125)
-l       name of file for log-information
-lynx    command for alternative browser
-mail    e-mail address to send results to
         (optionally append '#' or '\@\@'  and user name)
-mail_ascii  e-mail address to send text-only results to
         (optionally append '#' or '\@\@'  and user name)
-mail_results_format  specification for the format of result items
         that are to be sent by e-mail: Brief, Summary or XML
-mail_features  comma-separated list of extra features for the mail
         to be sent (without them it will be plain text). These are:
         css,javascript,entrez_links,pubcrawler_links,images,html,description
         or simply 'all' for everything
-mail_simple  e-mail address to send slim HTML results to
         (optionally append '#' or '\@\@'  and user name)
-mute    suppresses messages to STDERR
-n       URL where neighbourhood searches are directed to
-notify  e-mail address for notification
         (optionally append '#' or '\@\@'  and user name)
-no_test skips the proxy-test
-os      operating system (some badly configured versions of Perl need  
         this to be set explicitly -> 'MacOS', 'Win', and 'Unix')
-out     name of file for HTML-output
-p       proxy
-pp      proxy port
-pauth   proxy authorization (user name)
-ppass   proxy password
-pre     prefix used for default file names (config-file,database,log)
-q       URL where normal (not neighbourhood) searches are directed to
-r       specify a URL, that will be used for retrieving
         new hits from NCBI
-relentrezdate maximum age in days (relative date of Entrez-entry) 
         of a document to be retrieved
         other valid entries: '1 year','2 years','5 years','10 years','no limit'
-retry   specify how often PubCrawler should retry queries in case of
         server errors (default = 0)
-s       search-term ('database#alias#query#')
-spacer  location of gif that acts as space holder for left column
-t       timeout (in seconds, defaults to 180)
-u       test-URL (to test proxy configuration)
-v       verbose output
-viewdays number of days each document will be shown
-version show version of program
-bg      the bgd color to use
-proj    the name of the Api project
-icon    the project's icon

for more information see POD-text at the end of the script
or $home_link

    ***** This was the PubCrawler $version_number help message *****

";

# list of variables for which the program expects values:
my @expect_val = qw( 
             fullmax 
             getmax 
             html_file 
             include_config 
             relentrezdate 
             viewdays 
             bg
             proj
         );

my @allowed_var = qw(         
              break
              base_URL
              check
              cmd_query
              database
              extra_query
              extra_range
              fullmax 
              force_mail
              format
              from_quick
              from
              from_date
              header
              id
              indent
              log_file
              log_mail
              log_queries
              lynx
              mail
              mail_ascii
              mail_features
              mail_feature
              mail_only
              mail_relay
              mail_results_format
              mail_simple
              mute
              no_decap
              no_test         
              notify
              neighbour_URL
              pic
              prefix
              prompt
              proxy
              proxy_port
              proxy_auth
              proxy_pass
              quickstart
              relpubdate 
              replace_header 
              results_format
              retrieve_URL
              retry
              search_URL
              spacer
              system
              test_URL
              time_out
              to
              to_date
              tool
              touch
              verbose
              work_dir
);

my %PARAM = ();

# initialize parameters
foreach my $parameter (@expect_val, @allowed_var) {
    $PARAM{$parameter} = '';
}

# set defaults:
$PARAM{'time_out'} = 60;
$PARAM{'test_URL'} = 'http://www.ncbi.nlm.nih.gov/';
$PARAM{'retry'} = 0;          # retry in case of 'Server Error'
$PARAM{'break'} = '20';           # seconds of sleep between each request
$PARAM{'pic'} = 'http://pubcrawler.gen.tcd.ie/pics/pubcrawler_logo_new.jpg';
my $alias_global = '';
my $retrieve_URL_def = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi';
my $search_URL_def = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi';
#my $neighbour_URL_def = 'http://www.ncbi.nlm.nih.gov/entrez/utils/pmneighbor.fcgi';
my $neighbour_URL_def = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi';
my $relentrez_date_default = 180;
my %abstract_link = ('pubmed', 'http://www.ncbi.nlm.nih.gov:80/entrez/query.fcgi?cmd=Retrieve&dopt=Abstract&db=PubMed&list_uids=',
                     'nucleotide', 'http://www.ncbi.nlm.nih.gov/entrez/viewer.fcgi?db=nucleotide&val=');
my %checkbox_name = ('pubmed', '<INPUT TYPE="CHECKBOX" NAME="PubMedHits" VALUE=',
                     'nucleotide', '<INPUT TYPE="CHECKBOX" NAME="NucleotideHits" VALUE=');
my $summary_link = 'http://www.ncbi.nlm.nih.gov:80/entrez/query.fcgi?cmd=Retrieve&dopt=Summary';
my $related_article_link = 'http://www.ncbi.nlm.nih.gov:80/entrez/query.fcgi?db=PubMed&cmd=Display&dopt=pubmed_pubmed&from_uid=';

# colour definitions:
my $white = '#FFFFFF';
my $red = '#FF0000';

#arrays and list-variables:
my %age = ();         # age of each uid in days
my %aliases = ();     # queries ordered by alias
my %db = ();          # entries from database file
my %hits = ();        # number of hits for each alias
my %query = ();       # hash in which searches are stored
my @query_order = (); # order in which queries are specified
my @alias_order = (); # order in which aliases are specified
my @uid_list = ();    # list of PubMed Unique Identifiers 

# more variables:
my $alias = '';             # alias for search query
my $cmd_alias = '';         # alias for query entered from the command-line
my $cmd_db = '';            # database for command-line query
my $cmd_line_cfg_file = ''; # config file was specified on comand line
my $config_file = '';       # location of the configuration file
my $config_read = '';       # indicates if configuration file 
                            # has been read
my $copyright = '';         # show copyright if set
my $date_type = '';         # type of date (year, month, day)
my $dateline = '';          # headline of ouput with date
my $db = '';                # temporary holder of database key
my $db_change = 0;          # counts up if queries go to different database
my $err_msg = '';           # error messages from connections to NCBI
my $hostname = '';          # hostname
my $include_back = '';      # boolean for inclusion of back to top link
my $timezone = 'local time';       # Greenwich Mean Time or Irish Summer Time
my $joiner = '';            # OS-specific path-joiner
my $net_failure = '';       # indication if network failure occured
my $ncbi_form = '<form enctype="application/x-www-form-urlencoded" name="frmQueryBox" action="http://www.ncbi.nlm.nih.gov:80/entrez/query.fcgi?SUBMIT=y" method="POST" onSubmit="if(inGo!=true){document.frmQueryBox.cmd.value=\'\';}">';
                            # form address for simplified HTML results
my $old_dir = '';           # temporary holder of directory name
my $orig_system = '';       # original name given for OS
my $previous_db = '';       # stores value of database from previous query
my $prompted = '';          # set if Mac-users have been prompted
my $proxy_string = '';      # readily configured proxy
my $query = '';             # search query
my $query_out = '';         # temporary holder of original query string
my $result_collection = ''; # output text
my $result_collection_mail = ''; # output text
my $time_1 = '';            # for log purpose records time when mail/query starts
my $timestamp = '';         # holds time of creation of output
my $tmp_file = "/tmp/pubcrawler_tmp.$$";   # temporary file 
my $tmp_message = '';       # message presented in results file while PubCrawler runs
my $tmp_message_len = 0;    # length of $tmp_message
my $total_hits = 0;         # total number of all new hits (for notification)
my $trailer = '';           # appendix to output
my $update_counter = 0;     # percentage done
my $update_message = '';
my $update_message_len = 0;
my $update_thresh = '';
my $update_unit = 0;        # percentage added after each NCBI-connection
my $version = '';           # shows version if activated
my $log_id = '';
my $mail_log = '';
my $query_log = '';
my $mail_len = '';
my ($sec,$min,$hour,$mday,$mon,$year,$wday);
my $no_copy = '';
my $have_header = '';
my $help = '';
my $error = 0;    #needed for -check option
my @error = ();   #needed for -check option
my $warning = 0;  #needed for -check option
my @warning = (); #needed for -check option

    
# cascading style sheets:
my $css = '
<style type="text/css">
A:link  { color: navy; font-weight: text-decoration: none }
A:visited  { color: #5533FF; font-weight: text-decoration: none }
A:hover  { color: black; text-decoration: none }
A:active  { color: navy; text-decoration: none }
td  { font-size: 12px; font-family: Arial, Helvetica, Geneva, Swiss, SunSans-Regular }
.small  { font-size: 10px; line-height: 11px }
.pub { border: solid 8px #313063 }
.pub2 { border: solid 8px #cecfce }
h4 { text-indent: 10px }
h3 { text-indent: 10px }
</style>
';

    # javascript functions:
my $javascript = '
<script type="text/javascript">
<!--

function id_collect_ncbi(form)
{
  var allEmpty = true;
  var first = true;
  var url = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi";
  var db_list = form.NCBIDB;
  var db = db_list.options[db_list.selectedIndex].value;
  url = url + "?cmd=Retrieve&db=" + db + "&dopt=";
  var format_list = form.NCBIFormat;
  var format = format_list.options[format_list.selectedIndex].value;
  url = url + format + "&list_uids=";
  if (db == "pubmed") {
      if ( form.PubMedHits.length ) {
      // look for checked boxes;
      for ( index = 0; index < form.PubMedHits.length; index++ ) {
          if ( form.PubMedHits[ index ].checked ) {
          allEmpty = false; //at least one box ticked
              if (first == true) {
              url = url + form.PubMedHits[ index ].value;
              first = false;
              } else if (first == false) {
              url = url + "," + form.PubMedHits[ index ].value;
              } 
          }
      }
      } else {
      // if only one hit was found we can\'t treat the form as a list:
          if ( form.PubMedHits.checked ) {
          allEmpty = false; //at least one box ticked
          url = url + form.PubMedHits.value;
      }
      }
  } else {
      if ( form.NucleotideHits.length ) {
      // look for checked boxes;
      for ( index = 0; index < form.NucleotideHits.length; index++ ) {
          if ( form.NucleotideHits[ index ].checked ) {
          allEmpty = false; //at least one box ticked
              if (first == true) {
              url = url + form.NucleotideHits[ index ].value;
              first = false;
              } else if (first == false) {
              url = url + "," + form.NucleotideHits[ index ].value;
              } 
          }
      }
      } else {
      // if only one hit was found we can\'t treat the form as a list:
          if ( form.NucleotideHits.checked ) {
          allEmpty = false; //at least one box ticked
          url = url + form.NucleotideHits.value;
      }
      }
  }

  if ( allEmpty )
  {
    alert( "No checkbox selected!");
  }
  else
  {
    document.location=url;
// the above seems to work better
//    document.gotoNCBI.action=url;
//    document.gotoNCBI.submit();
  }
}

function CheckAll() {
  for (var i=0;i<document.myForm.elements.length;i++) {
    var e = document.myForm.elements[i];
    if (e.name != \'allbox\') {
      e.checked = document.myForm.allbox.checked;
    }
  }
}

function ToggleAll() {
  for (var i=0;i<document.myForm.elements.length;i++) {
    var e = document.myForm.elements[i];
    if (e.name != \'allbox\') {
      e.checked = (e.checked) ? false : true;
    }
  }
}


function setFormat(choice) {
    var isPreNN6 = (navigator.appName == "Netscape" && parseInt(navigator.appVersion) <= 4)
    switch (choice.value) {
        case "pubmed" :
        var formatList = new Array("Abstract", "DocSum", "Brief", "Citation", "MEDLINE", "XML", "ASN1", "ExternalLink")
        break
        case "nucleotide" :
        var formatList = new Array("GenBank", "DocSum", "Brief", "ASN1", "FASTA", "ExternalLink", "XML")
        break
        default:
        alert("Database \'" + choice + "\' not known!")
    }
    var listlength = formatList.length
    var listObj = document.forms[0].NCBIFormat
    // filter out old browsers
    if (listObj.type) {
    // empty options from list
    listObj.length = 0
    // create new option object for each entry
    for (var i = 0; i < listlength; i++) {
        listObj.options[i] = new Option(formatList[i])
    }
    listObj.options[0].selected = true
    if (isPreNN6) {
        history.go(0)
    }
    }
}

function ncbi_query(form) {

    var isPreNN6 = (navigator.appName == "Netscape" && parseInt(navigator.appVersion) <= 4);
    var allEmpty = true;
    var url = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=search&db=";
    var db_list = form.db;
    var db = db_list.options[db_list.selectedIndex].value;
    var search = form.term.value;
    url = url + db;
    
    if (search.length > 0) 
    {
    url = url + "&term=" + search;
    allEmpty = false;
    }

    if ( allEmpty )
    {
    alert( "No search term specified!");
    }
    else
    {
    document.gotoNCBI.action=url;
    document.gotoNCBI.submit();
    }
}
//-->
</script>
    ';
my $html_header = '';
my $html_header_mail = '';

# ask for command line options if running under MacOS
if (($PARAM{'system'} =~ /macos/i or $^O =~ /macos/i) and $PARAM{'prompt'} eq '1') {
    # we're running Macintosh and want to be prompted
    my( $cmdLine, @args );
    $cmdLine = &MacPerl::Ask( "Enter command line options (-h for help):" );
    require "shellwords.pl";
    @args = &shellwords( $cmdLine );
    unshift( @ARGV, @args );
    # make clear that we have been explicitely prompted:
    $prompted = '1';
}

# store command line options before retrieving them:
my @arg_tmp = @ARGV;
my $simulation = '';
my $simulation_file = '';
my $unused;
#### fetch command line options ####
GetOptions('add_path', \$PARAM{'add_path'},  # this option is dealt with in BEGIN{}
           'base_URL=s', \$unused,
           'replace_header', \$unused,
           'decap', \$unused,
           'break=i', \$PARAM{'break'},
           'h', \$help,
           'help', \$help,
           'head=s',\$PARAM{'header'},
           'c=s', \$config_file,
           'check', \$PARAM{'check'},
           'copyright', \$copyright,
           'd=s', \$PARAM{'work_dir'},
           'db=s', \$PARAM{'database'},
           'extra_query=s', \$PARAM{'extra_query'},
           'extra_range=i', \$PARAM{'extra_range'},
           'force_mail', \$PARAM{'force_mail'},
           'format|results_format=s', \$PARAM{'results_format'},
           'from_quick', \$PARAM{'from_quick'},
           'from|from_date=s', \$PARAM{'from_date'},
           'fullmax=s', \$PARAM{'fullmax'},
           'getmax=i', \$PARAM{'getmax'},
           'i', \$PARAM{'include_config'},
           'id=s', \$PARAM{'id'},
           'indent=i', \$PARAM{'indent'},
           'l=s', \$PARAM{'log_file'},
           'log', \$PARAM{'log_queries'},
           'lynx=s', \$PARAM{'lynx'},
           'mail_only=i', \$PARAM{'mail_only'},
           'mail=s', \$PARAM{'mail'},         
           'mail_ascii=s', \$PARAM{'mail_ascii'},
           'mail_features|mail_feature=s', \$PARAM{'mail_features'},
           'mail_simple=s', \$PARAM{'mail_simple'},
           'mail_results_format=s', \$PARAM{'mail_results_format'},
           'mog', \$PARAM{'log_mail'},
           'mute', \$PARAM{'mute'},
           'n=s', \$PARAM{'neighbour_URL'},
           'notify=s', \$PARAM{'notify'},
           'no_test', \$PARAM{'no_test'},
           'os=s', \$PARAM{'system'},
           'out=s', \$PARAM{'html_file'},
           'pic=s', \$PARAM{'pic'},
           'p=s', \$PARAM{'proxy'},
           'pp=i', \$PARAM{'proxy_port'},
           'pauth=s', \$PARAM{'proxy_auth'},
           'ppass=s', \$PARAM{'proxy_pass'},
           'pre=s', \$PARAM{'prefix'},
           'quickstart', \$PARAM{'quickstart'},
           'relentrezdate=s', \$PARAM{'relentrezdate'},
           'red=s', \$PARAM{'relentrezdate'},
           'relay|mail_relay=s', \$PARAM{'mail_relay'},
           'retry=i', \$PARAM{'retry'},
           'q=s', \$PARAM{'search_URL'},
           'r=s', \$PARAM{'retrieve_URL'},
           's=s', \$PARAM{'cmd_query'},
           'spacer=s', \$PARAM{'spacer'},
           't=i', \$PARAM{'time_out'},
           'to|to_date=s', \$PARAM{'to_date'},
           'tool=s', \$PARAM{'tool'},
           'touch=s', \$PARAM{'touch'},
           'u=s', \$PARAM{'test_URL'},
           'v', \$PARAM{'verbose'},
           'verbose', \$PARAM{'verbose'},
           'viewdays=i', \$PARAM{'viewdays'},
           'bg=s', \$PARAM{'bg'},
           'proj=s', \$PARAM{'proj'},
           'icon=s', \$PARAM{'icon'},
           'version', \$version,
           'simulation|sim|simulate=s', \$simulation,
           'sim_file|simulation_file=s', \$simulation_file,
       );


if ($help) {
    print STDERR "$USAGE";
    exit($EXIT_SUCCESS);
}

if ($version) {
    print STDERR "\nThis is PubCrawler version $version_number\n\n";
    exit($EXIT_SUCCESS);
}

if ($copyright) {
    print STDERR $gnugpl;
    exit($EXIT_SUCCESS);
}

# resolve '.' (current working directory)
$PARAM{'work_dir'} = $cwd if ($PARAM{'work_dir'} eq '.');

# default prefix is program name up to last dot
$PARAM{'prefix'} = $prog_name unless ($PARAM{'prefix'});

# SET SYSTEM DEPENDENCIES
$PARAM{'system'} = $^O unless ($PARAM{'system'});
$orig_system = $PARAM{'system'};
$PARAM{'system'} = 'macos' if ($PARAM{'system'} =~ /^mac|macintosh$/i);
$PARAM{'system'} = 'unix' if (grep /$PARAM{'system'}/i, @unix_flav);
#(this program doesn't distinguish between the 
# different UNIX-flavours) 

# configure system-dependent joiner for path names
if ($PARAM{'system'} =~ /macos/i) {
    $joiner = ':';
}elsif ($PARAM{'system'} =~ /unix/i){
    $joiner = '/';
}elsif ($PARAM{'system'} =~ /win/i) {
    $joiner = '\\';
} else {
    die "\nError: No valid operating system (OS) specified!
Please specify one of the following OS via comand line option '-os' :
 windows, mac, unix\n
(try '$program -h' for help)\n\n";
}
if ($PARAM{'work_dir'}) {
    $PARAM{'work_dir'} .= $joiner unless ($PARAM{'work_dir'} =~ /$joiner$/);
    if ($PARAM{'system'} =~ /win/i) {
    # special treatment for windows paths
    # (due to joiner symbol \)
    $PARAM{'work_dir'} =~ s/\\\\/\\/g;
    }
}

# READ IN CONFIGURATION FILE
# try config file specified on comand line 
if ($config_file) {
    $cmd_line_cfg_file = 1;
    if (-r $config_file) {
    &read_config;
    }
} else {
    # default configuration file consists of prefix + '.config'
    $config_file = "$PARAM{'prefix'}.config";
}

# next try any specified working directory
if (-r $PARAM{'work_dir'}.$config_file) {
    $config_file = $PARAM{'work_dir'}.$config_file;
    &read_config;
}

unless ($config_read) {
        # try the home-directory
    if (-r $ENV{'HOME'}.$joiner.$config_file) {
    $config_file = $ENV{'HOME'}.$joiner.$config_file;
    &read_config;
    } elsif (-r $cwd.$joiner.$config_file) {
    # try the current working directory    
    $config_file = $cwd.$joiner.$config_file;
    &read_config;
    } else {
    # configuration file cannot be read
    # die unless all mandatory variables are set
    if (&empty_vars(@expect_val)) {
        unless ($PARAM{'check'}) {
        print STDERR "$prog_name ERROR: Can not read configuration file \'$config_file\' from $cwd\ncommand line parameters were:";
        foreach (@arg_tmp) {
            print STDERR " $_";
        }
        print STDERR "\n\n";
                print "Please read instructions in readme file\nor at http://www.pubcrawler.ie!\n\n";
                sleep 3;
        exit;
        }
    } else {
        warn "$prog_name WARNING: Can not read configuration file \'$config_file\' from $cwd" unless ($PARAM{'check'});
    }
    }
}

unless ($PARAM{'tool'}) {
    $PARAM{'tool'} = "PubCrawler_$version_number";
}
$PARAM{'tool'} =~ s/\s+/+/g;

# extract mail features from comma-separated string:
my %mail_features = ();
my @mail_features = ('none','css','javascript','entrez_links','pubcrawler_links','images','html','text');
if ($PARAM{'mail_features'}) {
    foreach (split /,/, $PARAM{'mail_features'}) {
        $mail_features{$_}++;
        if ($_ eq 'all') {
            foreach (@mail_features) {
                $mail_features{$_}++;
            }
            last;
        }
        if ($_ eq 'none') {
            %mail_features = ();
            last;
        }
    }
}
my $no_html = 0;

# determine if mail file needs to be written:
my $create_mail_file = 0;
my $copy_mail_file = 0;
if ($PARAM{'mail'}
    or
    $PARAM{'mail_simple'}
    or
    $PARAM{'mail_ascii'}
    ) {

    # check if some of the mail features are NOT defined:
    # this would be particularly true for versions of pubcrawler prior to version 2
    foreach (@mail_features) {
        unless ($mail_features{$_}) {
            # ... in which case a mail file needs to be created
            $create_mail_file = 1;
            last;
        }
    }

    unless ($create_mail_file) {
        # all mail features activated
        # this will results in the standard output file
        # to be sent by mail
        $copy_mail_file = 1;
    }
}

my $boldIn = '<B>';
my $boldOut = '</B>';
my $itIn = '<I>';
my $itOut = '</I>';
my $h4In = '<H4>';
my $h4Out = '</H4>';
my $ulIn = '<UL>';
my $ulOut = "\n</UL>";
my $liIn = "\n<LI>";
my $liOut = '</LI>';
my $break = "\n<BR>";
my $space = '&nbsp;';
my $rarr = '&rarr;';
unless (defined $mail_features{'html'}) {
    $boldIn = '';
    $boldOut = '';
    $itIn = '';
    $itOut = '';
    $h4In = '';
    $h4Out = '';
    $ulIn = '';
    $ulOut = '';
    $liIn = "\n- ";
    $liOut = '';
    $break = "\n";
    $space = ' ';
    $rarr = '->';
}

print STDERR "\nStarting PubCrawler version $version_number, Copyright (C) 1999 - 2004 K.H. Wolfe, K. Hokamp\n
PubCrawler comes with ABSOLUTELY NO WARRANTY.
This is free software, and you are welcome
to redistribute it under certain conditions;
use option '-copyright' for details.\n\n" unless ($PARAM{'mute'});

# all mandatory variables are set now
# searches are stored in hash %query
# hash %aliases contains searches ordered by alias


# ask Mac-users for command line options again 
# (only in case the configuration file 
# that was just read in demands so...)
# -> gives a chance to overwrite or set new values
#    for selected variables
if (($PARAM{'system'} =~ /macos/i or $^O =~ /macos/i) and $PARAM{'prompt'} eq '1' and $prompted ne '1') {
    # we're running Macintosh and want to be prompted
    my( $cmdLine, @args );
    $cmdLine = &MacPerl::Ask( "Enter command line options (-h for help):" );
    require "shellwords.pl";
    @args = &shellwords( $cmdLine );
    unshift( @ARGV, @args );


    #### fetch command line options again####
    GetOptions('h', \$help,
           'help', \$help,
           'head=s',\$PARAM{'header'},
           'c=s', \$config_file,
           'check', \$PARAM{'check'},
               'copyright', \$copyright,
           'd=s', \$PARAM{'work_dir'},
           'db=s', \$PARAM{'database'},
           'extra_query=s', \$PARAM{'extra_query'},
           'extra_range=i', \$PARAM{'extra_range'},
               'force_mail', \$PARAM{'force_mail'},
               'format|results_format=s', \$PARAM{'results_format'},
               'from_quick', \$PARAM{'from_quick'},
               'from=s', \$PARAM{'from_date'},
           'fullmax=s', \$PARAM{'fullmax'},
           'getmax=i', \$PARAM{'getmax'},
           'i', \$PARAM{'include_config'},
               'id=s', \$PARAM{'id'},
               'indent=i', \$PARAM{'indent'},
           'l=s', \$PARAM{'log_file'},
           'lynx=s', \$PARAM{'lynx'},
           'mail=s', \$PARAM{'mail'},
               'mail_ascii=s', \$PARAM{'mail_ascii'},
               'mail_features=s', \$PARAM{'mail_features'},
           'mail_simple=s', \$PARAM{'mail_simple'},
               'mail_results_format=s', \$PARAM{'mail_results_format'},
           'mute', \$PARAM{'mute'},
               'n=s', \$PARAM{'neighbour_URL'},
           'notify=s', \$PARAM{'notify'},
           'no_test', \$PARAM{'no_test'},
           'os=s', \$PARAM{'system'},
           'out=s', \$PARAM{'html_file'},
           'p=s', \$PARAM{'proxy'},
           'pp=i', \$PARAM{'proxy_port'},
           'pauth=s', \$PARAM{'proxy_auth'},
           'ppass=s', \$PARAM{'proxy_pass'},
           'pre=s', \$PARAM{'prefix'},
           'quickstart', \$PARAM{'quickstart'},
           'relentrezdate=s', \$PARAM{'relentrezdate'},
           'red=s', \$PARAM{'relentrezdate'},
               'relay|mail_relay=s', \$PARAM{'mail_relay'},
               'retry=i', \$PARAM{'retry'},
           'q=s', \$PARAM{'search_URL'},
           'r=s', \$PARAM{'retrieve_URL'},
           's=s', \$PARAM{'cmd_query'},
               'spacer=s', \$PARAM{'spacer'},
           't=i', \$PARAM{'time_out'},
               'tool=s', \$PARAM{'tool'},
               'to=s', \$PARAM{'to_date'},
               'touch=s', \$PARAM{'touch'},
           'u=s', \$PARAM{'test_URL'},
           'v', \$PARAM{'verbose'},
           'verbose', \$PARAM{'verbose'},
           'viewdays=i', \$PARAM{'viewdays'},
           'version', \$version
           );

    if (defined $help) {
    print STDERR "$USAGE";
    exit($EXIT_SUCCESS);
    }
    if ($version) {
    print STDERR "\nThis is PubCrawler version $version_number\n\n";
    exit($EXIT_SUCCESS);
    }
    
    # resolve '.' (current working directory)
    $PARAM{'work_dir'} = cwd if ($PARAM{'work_dir'} eq '.');
    
    # default prefix is program name up to last dot
    $PARAM{'prefix'} = $prog_name unless ($PARAM{'prefix'});
}

$PARAM{'icon'} = $PARAM{'icon'} || $PARAM{'spacer'} || 'http://pubcrawler.gen.tcd.ie/pics/spacer.gif';

# modify query URLs if extra_query was specified
if ($PARAM{'extra_query'}) {
    foreach (keys %abstract_link) {
        $abstract_link{$_} =~ s/\?/\?$PARAM{'extra_query'}\&/;
    }
    $summary_link =~ s/\?/\?$PARAM{'extra_query'}\&/;
    $related_article_link =~ s/\?/\?$PARAM{'extra_query'}\&/;
    $ncbi_form =~ s/\?/\?$PARAM{'extra_query'}\&/;
    $javascript =~ s/\?cmd=Retrieve/\?$PARAM{'extra_query'}\&cmd=Retrieve/;
    $javascript =~ s/\?cmd=search/\?$PARAM{'extra_query'}\&cmd=search/;
    $PARAM{'extra_query'} = '?'.$PARAM{'extra_query'};
}

# slim HTML file if mail_ascii was specified
$PARAM{'mail_simple'} = '1' if ($PARAM{'mail_ascii'});

if ($PARAM{'id'} ne '') {
    if ($PARAM{'log_queries'} or $PARAM{'log_mail'}) {
    $log_id = $PARAM{'id'};
    }
#    $PARAM{'id'} = "for $PARAM{'id'}";
}

#formats for NCBI reports
my @formats = ('Summary','DocSum','Brief','Abstract','Citation','MEDLINE','ASN.1','ExternalLink','GenBank','FASTA', 'XML');
unless (grep /^$PARAM{'results_format'}$/i, @formats) {
    if ($PARAM{'results_format'} eq '') {
        $PARAM{'results_format'} = 'DocSum';
    } else {
        print STDERR "$PARAM{'id'} format $PARAM{'results_format'} not supported - using 'DocSum' instead!\n" unless (
                                       $PARAM{'mute'}
                                       or
                                       $PARAM{'results_format'} =~ /Summary/i);
        $PARAM{'results_format'} = 'DocSum';
    }
}

# set mail_results_format to the same as the web output format
# unless defined
if ($PARAM{'mail_results_format'} eq ''
    or
    $PARAM{'mail_results_format'} =~ /same/i) {
    $PARAM{'mail_results_format'} = $PARAM{'results_format'} ;
}


# set value of extra_range to $full_max or 1
unless ($PARAM{'extra_range'} =~ /^\d+$/) {
    $PARAM{'extra_range'} = $PARAM{'fullmax'};
}
$PARAM{'extra_range'} = 1 if ($PARAM{'extra_range'} eq ''
                              or
                              $PARAM{'extra_range'} < 1);

# use cwd as working directory
# if $PARAM{'work_dir'} has no value
unless ($PARAM{'work_dir'}) {
    $PARAM{'work_dir'} = $cwd;
    printf STDERR "\n\n** $prog_name: using $PARAM{'work_dir'} as working directory! **\n\n" unless ($PARAM{'check'} or $PARAM{'mute'});
}

# special treatment of variable 'relentrezdate'
if ($PARAM{'relentrezdate'} eq '') {
    if ($PARAM{'relpubdate'}) {
        $PARAM{'relentrezdate'} = $PARAM{'relpubdate'};
    } else {
        $PARAM{'relentrezdate'} = $relentrez_date_default;
    }
}

if ($PARAM{'relentrezdate'} !~ /\d/i) {
    $PARAM{'relentrezdate'} = 100000;
    print STDERR "$PARAM{'id'} Setting relentrezdate to no limit!\n" unless ($PARAM{'mute'});
} elsif ($PARAM{'relentrezdate'} =~ /^\s*(\d+)\s*(\D+)/) {
    $PARAM{'relentrezdate'} = $1;
    $date_type = $2;
    if ($date_type =~ /^y/i) {
    $PARAM{'relentrezdate'} *= 365;
    } # otherwise assuming days
}

# set default values if necessary:
my $spacer_gif = $PARAM{'spacer'} || 'http://pubcrawler.gen.tcd.ie/pics/spacer.gif';
$PARAM{'retrieve_URL'} = $retrieve_URL_def if ($PARAM{'retrieve_URL'} eq '');
$PARAM{'search_URL'} = $search_URL_def if ($PARAM{'search_URL'} eq '');
$PARAM{'neighbour_URL'} = $neighbour_URL_def if ($PARAM{'neighbour_URL'} eq '');

##################################
#### END OF VARIABLE SETTING #####
##################################

&check_setting if ($PARAM{'check'});

#### check that all mandatory fields have values ####
####        and write them to the log file       ####
if (@_ = &empty_vars(@expect_val,'system')) {
    print STDERR "$prog_name ERROR $PARAM{'id'}: no value set for the following variable(s):\n";
    foreach (@_) {
    print STDERR "\t$_\n";
    }
    print STDERR "\nPlease check your configuration file or use command line options!\n";
    exit($EXIT_FAILURE);
}

# try to change to working directory
chdir "$PARAM{'work_dir'}" or 
    die "$prog_name ERROR: Can not change to working directory \'$PARAM{'work_dir'}\'";


my $ip = '';
if ($PARAM{'mail_only'} =~ /\d/) {
    if ($ip eq '') {
    $ip = `hostname -i`;
    # use only first part in case more than one are specified
    ($ip) = split / /, $ip;
    unless ($? == 0) {
        $ip = `hostname`;
    }
    }
    $ip =~ s/\s$//g;
    ($mday,$mon,$year) = (localtime(time))[3,4,5];
    $mon++;
    $year += 1900;
    foreach ($mday,$mon){
    if (length($_) == 1) {
        $_ = "0".$_;
    }
    }
    
    $mail_log = "/tmp/mails_$year$mon${mday}_$ip";
    unless (-e $mail_log) {
    system "touch $mail_log; chmod 666 $mail_log";
    }
    unless (-w $mail_log) {
    warn "Can't write to $mail_log: $?\n";
    $PARAM{'log_mail'} = '';
    }

    &mail_service;

    # delete any temporary files
    if (-e $tmp_file) {
    sleep 5;
    unlink $tmp_file;
    }
    if (-e $tmp_file.'.mail') {
        sleep 5;
        unlink $tmp_file.'.mail';
    }

    if (-e $PARAM{'html_file'}.'.simple.mail') {
        sleep 5;
        unlink $PARAM{'html_file'}.'.simple.mail';
    }
    exit;
} else {
    # delete previous mail file unless mail_only was specified
    if (-e "$PARAM{'html_file'}.mail") {
        unlink "$PARAM{'html_file'}.mail";
    }
}

   
#### open log file ####
unless ($PARAM{'verbose'}) {        
    $PARAM{'log_file'} = "$PARAM{'prefix'}_log.html" unless ($PARAM{'log_file'});
    open (LOGFILE,">$PARAM{'log_file'}") ||
    die "$prog_name ERROR:cannot open log file ($PARAM{'log_file'}):$!";
    select (LOGFILE);
    print "<HTML><title>PubCrawler log file</title>
          <h2>PubCrawler logfile</h2><pre>";
}

print "config file is $config_file in $PARAM{'work_dir'}\n\n";

# try to connect directly first
print "testing for direct internet connection...";
if (&connection_test('no_proxy',$PARAM{'test_URL'})) {
    print "successful!\n";
    unless ($PARAM{'proxy'} eq '') {
        print "Disabling proxy (not necessary)!\n";
        $PARAM{'proxy'} = '';
    }
}
# configure and test proxy settings
# if value for proxy is specified
if ($PARAM{'proxy'}) {
    print "no success, getting proxy...\n";
    foreach my $proxy_tmp (split /,/, $PARAM{'proxy'}) {
        &proxy_setting($proxy_tmp);      
    }
}

# overwrite queries from config-file
# if query was given on the command-line:
if ($PARAM{'cmd_query'}) {
    ($cmd_db, $cmd_alias, $PARAM{'cmd_query'}) = split /#/, $PARAM{'cmd_query'};
    $PARAM{'cmd_query'} = $cmd_alias unless ($PARAM{'cmd_query'});
    my $cmd_query_orig = $PARAM{'cmd_query'};
    $PARAM{'cmd_query'} =~ s/\[ *all( fields)? *\]//gi;
    $PARAM{'cmd_query'} =~ s/\s+/\+/g;    #put in plusses
    $PARAM{'cmd_query'} = uc $PARAM{'cmd_query'};
    @query_order = ($PARAM{'cmd_query'});
    @alias_order = ($cmd_alias);
    %aliases = ();
    %query = ();
    push @{ $aliases{$cmd_alias} }, $PARAM{'cmd_query'};
    $query{$PARAM{'cmd_query'}}{'ALIAS'} = $cmd_alias;      
    $query{$PARAM{'cmd_query'}}{'DB'} = $cmd_db;
    $query{$PARAM{'cmd_query'}}{'ORIG'} = $cmd_query_orig;
}

# list all searches for log:
print "\n";
foreach (@allowed_var) {print " $_ : $PARAM{$_}\n";}
print "\n searches:\n";
foreach (@query_order) { print "\t$_ at $query{$_}{'DB'}\n";}
print "\n changing dir to $PARAM{'work_dir'} \n\n";


# calculate size of $update_unit;
if (@query_order == 0) {
     print STDERR "$prog_name WARNING $PARAM{'id'}: empty set of queries!\n";
} else {
     $update_unit = sprintf "%.1f",100 / (2 * @query_order);
}
$update_message = "<B>1\%</B> done.<BR>";
$update_message_len = length($update_message);

# make backup of output file:
if (-e "$PARAM{'html_file'}.bak") {
    unlink "$PARAM{'html_file'}.bak";
}

my $backup_file = '';
if (-e "$PARAM{'html_file'}") {
    unless ($no_copy) {
        copy("$PARAM{'html_file'}","$PARAM{'html_file'}.bak");
        $backup_file = basename($PARAM{'html_file'}).'.bak';
    }
}

#### prepare output HTML file with header and trailer ####
if (-e $PARAM{'html_file'}) {unlink $PARAM{'html_file'};}

#begin writing to output file:
sysopen (OUT,"$PARAM{'html_file'}",O_WRONLY|O_TRUNC|O_CREAT) ||
    die "$prog_name ERROR: could not write HTML output to file $PARAM{'html_file'} $PARAM{'id'}\n";

# start collecting output for mail file
my $mail_results_file = '';
my $full_records_mail = '';

#### write beginning of results and mail file:
&start_results_file;


######################################
#########  read in database  #########
######################################
&read_db;

# leave signature on nfs-mounted partition
# (feature used within www-service only)
if ($PARAM{'touch'}) {
    $ip = `hostname -i`;
    # use only first part in case more than one are specified
    ($ip) = split / /, $ip;
    unless ($? == 0) {
    $ip = `hostname`;
    }
    system "touch $PARAM{'touch'}/$ip";
}

if ($PARAM{'log_queries'}) {
    if ($ip eq '') {
    $ip = `hostname -i`;
    # use only first part in case more than one are specified
    ($ip) = split / /, $ip;
    unless ($? == 0) {
        $ip = `hostname`;
    }
    }
    $ip =~ s/\s$//g;
    ($mday,$mon,$year) = (localtime(time))[3,4,5];
    $mon++;
    $year += 1900;
    foreach ($mday,$mon){
    if (length($_) == 1) {
        $_ = "0".$_;
    }
    }
    
    $query_log = "/tmp/queries_$year$mon${mday}_$ip";
    unless (-e $query_log) {
    system "touch $query_log; chmod 666 $query_log";
    }
    unless (-w $query_log) {
    warn "Can't write to $query_log: $?\n";
    $PARAM{'log_queries'} = '';
    }
}
    
if ($PARAM{'log_mail'}) {
    if ($ip eq '') {
    $ip = `hostname -i`;
    # use only first part in case more than one are specified
    ($ip) = split / /, $ip;
    unless ($? == 0) {
        $ip = `hostname`;
    }
    }
    $ip =~ s/\s$//g;
    ($mday,$mon,$year) = (localtime(time))[3,4,5];
    $mon++;
    $year += 1900;
    foreach ($mday,$mon){
    if (length($_) == 1) {
        $_ = "0".$_;
    }
    }
    
    $mail_log = "/tmp/mails_$year$mon${mday}_$ip";
    unless (-e $mail_log) {
    system "touch $mail_log; chmod 666 $mail_log";
    }
    unless (-w $mail_log) {
    warn "Can't write to $mail_log: $?\n";
    $PARAM{'log_mail'} = '';
    }
}


# if a date range was specified, construct string
# that goes into query URL:
my $date_range = '';
my $date_range_msg = '';
if ($PARAM{'from_date'} =~ /\d{4,}/) {
    $date_range_msg = "<H4>Date range: from $PARAM{'from_date'} to ";
    $date_range = '&mindate='.(substr $PARAM{'from_date'}, 0, 4,'');
    if (length($PARAM{'from_date'}) > 1) {
    $date_range .= '/'.(substr $PARAM{'from_date'}, 0, 2,'');
    if (length($PARAM{'from_date'}) > 1) {
        $date_range .= '/'.(substr $PARAM{'from_date'}, 0, 2,'');
    } else {
        $date_range = '';
        $date_range_msg = '';
    }
    } else {
    $date_range = '';
    $date_range_msg = '';
    }
}
unless ($PARAM{'to_date'} =~ /\d{4,}/) {
    if ($date_range) {
        # get today's date:
        my ($tmp_y,$tmp_m,$tmp_d) = (localtime)[5,4,3];
        $tmp_y += 1900;
        $tmp_m += 1;
        $tmp_m = sprintf ("%0.2d", $tmp_m);
        $tmp_d = sprintf ("%0.2d", $tmp_d);
        $PARAM{'to_date'} = $tmp_y.$tmp_m.$tmp_d;
    }
}

if ($PARAM{'to_date'} =~ /\d{4,}/) {
    $date_range_msg .= "$PARAM{'to_date'}</H4>";
    $date_range .= '&maxdate='.(substr $PARAM{'to_date'}, 0, 4,'');
    if (length($PARAM{'to_date'}) > 1) {
    $date_range .= '/'.(substr $PARAM{'to_date'}, 0, 2,'');
    if (length($PARAM{'to_date'}) > 1) {
        $date_range .= '/'.(substr $PARAM{'to_date'}, 0, 2,'');
    } else {
        $date_range = '';
        $date_range_msg = '';
    }
    } else {
    $date_range = '';
    $date_range_msg = '';
    }
}    
my $date_range_msg_mail = $date_range_msg;

##################################################
#### make first visit to NCBI to get all UIDs ####
##################################################
foreach $query (@query_order) {

    %age = (); # values of age will be filled in sub first_visit
    $db = $db_match{$query{$query}{'DB'}};

    # first visit to NCBI for each query to get UIDs
    my $hits = 0;
    if ($simulation) {
    @uid_list = split /,/, $simulation;
    $hits = @uid_list;
    $err_msg = '';
    } elsif ($simulation_file) {
    $hits = 2;
    @uid_list = (1,1);
    $PARAM{'break'} = 0;
    } else {
    ($hits,
     $err_msg,
     @uid_list) = &first_visit($db,$query);
    }

        # space requests at $PARAM{'break'} seconds interval...
    unless ($query eq $query_order[-1]) {
        print "sleeping for $PARAM{'break'} seconds...\n";
        sleep($PARAM{'break'});
    }
    
    # add results to hash %query
    $query{$query}{'HITS'} = $hits;
    $query{$query}{'ERR'} = $err_msg;
    @{ $query{$query}{'UIDS'} } = @uid_list;
}

print "\n====\nFinished first visit to NCBI.\n====\n";

###################################################
############ make second visit to NCBI ############
##### to get full reports of interesting UIDs #####
###################################################
my $result_mail_records = '';
my %next_alias = ();
for (my $i = 0; $i <= $#alias_order; $i++) {
    $alias = $alias_order[$i];

    if (defined $alias_order[$i+1]) {
        $next_alias{$alias} = $alias_order[$i+1];
    }

    my $word = '';
    @uid_list = ();
    my @getmax_warning = ();
    my @hit_numbers = ();
    my @getmax_warning_mail = ();
    my @hit_numbers_mail = ();
    my $sequence_search = 0;
    $db = '';
    $query = '';

    # combine retrieved uids from each query
    # that has the same alias:
    my $warn_entry = 0;
    my $first_visit_error = 0;
    foreach $query ( @{ $aliases{$alias} } ) {
    $query_out = "'".$query{$query}{'ORIG'}." '";

    if ($query{$query}{'ERR'}) {

        # an erorr was returned from the first query
        push @hit_numbers, "No hits for $query_out\n<BR><B>Message from NCBI:</B> ".$query{$query}{'ERR'};
        push @hit_numbers_mail, "No hits for $query_out${break}${boldIn}Message from NCBI:${boldOut} ".$query{$query}{'ERR'};
        $word = $word{$query{$query}{'DB'}} unless ($word);
            $first_visit_error++;

    } else {

        # more hits available than limit:
        if ($query{$query}{'HITS'} >= $PARAM{'getmax'}) {

        # report number of hits after first visit
            push @hit_numbers, "$query{$query}{'HITS'} hit".($query{$query}{'HITS'}==1?'':'s')." after <B>first</B> visit for $query_out";
        push @hit_numbers, "<B>Warning:</B> retrieved max number of items for this query!";
            push @hit_numbers_mail, "$query{$query}{'HITS'} hit".($query{$query}{'HITS'}==1?'':'s')." after ${boldIn}first${boldOut} visit for $query_out";
        push @hit_numbers_mail, "${boldIn}Warning:${boldOut} retrieved max number of items for this query!";

        $warn_entry++;

        } elsif ($query{$query}{'HITS'} > 0) {

        # report number of hits after first visit
                # only when the second query produced results
            push @hit_numbers, "$query{$query}{'HITS'} hit".($query{$query}{'HITS'}==1?'':'s')." after <B>first</B> visit for $query_out";
            push @hit_numbers_mail, "$query{$query}{'HITS'} hit".($query{$query}{'HITS'}==1?'':'s')." after ${boldIn}first${boldOut} visit for $query_out";
        }
        push @uid_list, @{ $query{$query}{'UIDS'} };
        $db = $query{$query}{'DB'} unless ($db);
        $word = $word{$query{$query}{'DB'}} unless ($word);
    }
    }

    # at this stage, $db is one of the known_searchtypes
    # (pubmed|genbank|nucleotide|pm_neighbour|gb_neighbour)
    # whatever was encountered first, in case of multiple queries for the same alias
    my $neighbour_hood_search = 0;
    if ($db =~ /_neighbour/) {
    $neighbour_hood_search = 1;
    } elsif ($db eq 'nucleotide') {
    $sequence_search = 1;
   }
    
    my $warn_text = '';
    my $warn_text_mail = '';
    my $warn_insert = 'older';
    if (-s $PARAM{'database'}
    or
    -s "$PARAM{'prefix'}.db") {
    if ($neighbour_hood_search) {
        $warn_insert = 'less significant';
    }
    $warn_text = "${rarr}${space}Increase value of <I>getmax</I> (currently $PARAM{'getmax'}) via command-line option or in configuration file for additional ($warn_insert) results.\n";
    $warn_text_mail = "${rarr}${space}Increase value of ${itIn}getmax${itOut} (currently $PARAM{'getmax'}) via command-line option or in configuration file for additional ($warn_insert) results.\n";
    } else {
    $warn_text = "<BR>\n${rarr}${space}The number of results exceeded <I>getmax</I> (currently $PARAM{'getmax'}), this is not unusual for database initialisation. It means, however, that some $warn_insert reports are not shown.\n";
    $warn_text_mail = "<BR>\n${rarr}${space}The number of results exceeded ${itIn}getmax${itOut} (currently $PARAM{'getmax'}), this is not unusual for database initialisation. It means, however, that some $warn_insert reports are not shown.\n";
    }

    if ($warn_entry) {
    # there was at least one query that hit the limit
    push @hit_numbers, "<BR>$warn_text";
    push @getmax_warning, $warn_text;
    push @hit_numbers_mail, "${break}$warn_text_mail";
    push @getmax_warning_mail, $warn_text_mail;
    }

    my @status_message = ();
    my @status_message_mail = ();

    my ($result,$result_mail);

    if (@uid_list) {    
       # extract all suitable uids
    print "\ndetermining new reports for $alias\n";
    @uid_list = &list_crunch($alias,@uid_list);
    # @uid_list could be empty now
    # (if all entries are older than viewdays + 3)
    # but &second_visit will deal with this...
    print "\n=====\n$alias:\nmaking second HTTP connection to retrieve complete records.\n";
       # retrieve full records and
           # space requests at $PARAM{'break'} seconds interval...
        print "sleeping for $PARAM{'break'} seconds...\n";
        sleep($PARAM{'break'});
    $db = 'nucleotide' if ($db eq 'genbank');
    $db = $back_match{$db} if ($db =~ /_neighbour/);

    # at this stage, db can be either of
    # 'pubmed' or 'nucleotide'

    ($result,$result_mail) = &second_visit($alias,$db,$word,@uid_list);
        $result_mail_records .= $result_mail;

    } else {

    # No hits after first visit

    $result .=
        &tab('TR')
        .&tab('TD');
        
        $no_html = &set_flag;
    $result_mail .=
        &tab('TR')
        .&tab('TD');
        $no_html = 0;
        
    $hits{$alias} = 0;
    if (@getmax_warning) {
        push @status_message, "No new records retrieved for \'$alias\'", @getmax_warning;
        push @status_message_mail, "No new records retrieved for \'$alias\'", @getmax_warning_mail;
    } elsif ($query{@{ $aliases{$alias} }[0]}{'ERR'} =~ /Neighbourhood search for GenBank <B>currently disabled<\/B>/) {
        push @status_message, $query{@{ $aliases{$alias} }[0]}{'ERR'};
        push @status_message_mail, $query{@{ $aliases{$alias} }[0]}{'ERR'};
    } elsif ($query{@{ $aliases{$alias} }[0]}{'ERR'} =~ /No Documents Found/
         or
         $query{@{ $aliases{$alias} }[0]}{'ERR'} !~ /\w/) {
        push @status_message, "No matching documents found in Entrez for the last <B>$PARAM{'relentrezdate'} days</B>";
        push @status_message_mail, "No matching documents found in Entrez for the last ${boldIn}$PARAM{'relentrezdate'} days${boldOut}";
    } elsif ($query{@{ $aliases{$alias} }[0]}{'ERR'} =~ /\w/) {
        push @status_message, "The following error message was received: $query{@{ $aliases{$alias} }[0]}{'ERR'}";
        push @status_message_mail, "The following error message was received: $query{@{ $aliases{$alias} }[0]}{'ERR'}";
    } else {
        push @status_message, "No new records retrieved for \'$alias\'. ".'<B>Possible network failure.</B>';
        push @status_message_mail, "No new records retrieved for \'$alias\'. ".'${boldIn}Possible network failure.${boldOut}';
        if ($PARAM{'quickstart'}) {
        push @status_message, "<B>-\&gt<A HREF=\"http://pubcrawler.gen.tcd.ie/quickstart.html\">Restart your jobs manually\!</A></B>";
                my $text_insert = "<A HREF=\"http://pubcrawler.gen.tcd.ie/quickstart.html\">Restart your jobs manually\!</A>";
                unless ($mail_features{'html'}) {
                    $text_insert = "Restart your jobs manually at http://pubcrawler.gen.tcd.ie/quickstart.html";
                }
        push @status_message_mail, "${boldIn}${rarr}$text_insert${boldOut}";
        $net_failure = 1;
        }
    }
    push @status_message, "<BR>Database for this entry not updated.\n";
    push @status_message_mail, "${break}Database for this entry not updated.\n";
    $result .= 
        ${space}
        .&tab('/TD')
        .&tab('/TR');

        $no_html = &set_flag;
    $result_mail .= 
        ${space}
        .&tab('/TD')
        .&tab('/TR');
        $no_html = 0;
    }


    ### write search results and error messages to output file:

    $indent = 6;
    $prev_indent = '-';

    my $quot_alias = $alias;
    $quot_alias =~ s/\"/&quot;/g;
    
    $result_collection .= "\n<!-- Results for $alias -->"
    .&tab('TR'," bgcolor=\"$PARAM{'bg'}\"")
    .&tab('TD')
    ."<H3><font color=\"#ffffff\">::::::</font>&nbsp;<A NAME=\"$quot_alias\">Results for \'$alias\' at $word</A></H3>"
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR')
    .&tab('TD')
    .&tab('TABLE',' bgcolor="#efefef" WIDTH="100%"')
    .&tab('TR')
    .&tab('TD')
    .'&nbsp;'
    .&tab('/TD')
    .&tab('TD'); # grey box for status messages
    
    my $text_insert = "<H3><font color=\"#ffffff\">::::::</font>&nbsp;<A NAME=\"$quot_alias\">Results for \'$alias\' at $word</A></H3>";
    unless (defined $mail_features{'html'}) {
        $text_insert = "\n:::::: Results for \'$alias\' at $word\n\n";
    }

    $no_html = &set_flag;
    $result_collection_mail .= "\n"
    .&tab('TR'," bgcolor=\"$PARAM{'bg'}\"")
    .&tab('TD')
    .$text_insert
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR')
    .&tab('TD')
    .&tab('TABLE',' bgcolor="#efefef" WIDTH="100%"')
    .&tab('TR')
    .&tab('TD')
    .${space}
    .&tab('/TD')
    .&tab('TD'); # grey box for status messages
     $no_html = 0;


     # add links to next section
     if ($#alias_order > 0) {
         my $add = "<CENTER>"
            .(defined $next_alias{$alias} ? 
               "<A HREF=\"#$next_alias{$alias}\">[next]</A>&nbsp;" 
             : "<A HREF=\"#retrieval\">[next]</A>&nbsp;" )
            ."<A HREF=\"#TOP\">[top]</A></CENTER>\n";
         $result_collection .= $add;
         if ($mail_features{'html'}) {
             $result_collection_mail .= $add;
         }      
    }

    if ($hits{$alias}) {
    $result_collection .= join "<BR>\n", @hit_numbers; 
    $result_collection_mail .= join "${break}", @hit_numbers_mail; 
    } else {
    $result_collection .= join "<BR>\n", @status_message;
    $result_collection_mail .= join "${break}", @status_message_mail;
    }

    $result_collection .=
    &tab('/TD')
    .&tab('TD')
    .'&nbsp;'
    .&tab('/TD')
    .&tab('/TR')
    .&tab('/TABLE') # this closes off the table with the grey status bar
    .&tab('/TD')
    .&tab('/TR');

    $no_html = &set_flag;
    $result_collection_mail .=
    &tab('/TD')
    .&tab('TD')
    .${space}
    .&tab('/TD')
    .&tab('/TR')
    .&tab('/TABLE') # this closes off the table with the grey status bar
    .&tab('/TD')
    .&tab('/TR');
    $no_html = 0;

    if ($hits{$alias}) {

    my $sorted_by = '';
    if ($hits{$alias} > 2) {
        $sorted_by = ', sorted by ';
        if ($neighbour_hood_search) {
        $sorted_by .= 'significance';
        } else {
                if ($sequence_search) {
            $sorted_by .= 'Modification date';
                } else {
            $sorted_by .= 'Entrez date';
                }
        }
    }

    $result_collection .=
        &tab('TR',' bgcolor="LIGHTGREY"')
        .&tab('TD')
        ."<h4>&nbsp;Today\'s new results ($hits{$alias} item"
        .($hits{$alias} > 1 ? 's' : '')
        ." in total"
        .$sorted_by
        ."):</h4>"
        .&tab('/TD')
        .&tab('/TR'); # this closes off the dark grey row 

        $no_html = &set_flag;
    $result_collection_mail .=
        &tab('TR',' bgcolor="LIGHTGREY"')
        .&tab('TD')
        ."\n${h4In}${space}Today\'s new results ($hits{$alias} item"
        .($hits{$alias} > 1 ? 's' : '')
        ." in total"
        .$sorted_by
        ."):${h4Out}"
        .&tab('/TD')
        .&tab('/TR'); # this closes off the blue row 
         $no_html = 0;
    }

    # listing of hits:
    $result_collection .= $result;  
    $result_collection_mail .= $result_mail;    
    
    # add a spacer line
    $result_collection .= 
    &tab('TR')
    .&tab('TD');

    $no_html = &set_flag;
    $result_collection_mail .= 
    &tab('TR')
    .&tab('TD');
    $no_html = 0;

    # back to top link after each section
#    $text_insert = 
#       &tab('TR')
#       .&tab('TD')
#       ."<CENTER><A HREF=\"#TOP\">[back to top]</A></CENTER><BR>\n"
#       .&tab('/TD')
#       .&tab('/TR');
#   
#    $result_collection .= $text_insert;
#    unless (defined $mail_features{'html'}) {
#        $text_insert = '';
#    }
#
#    $no_html = &set_flag;
#    $result_collection_mail .= $text_insert;
#    $no_html = 0;
   
    $result_collection .= '<BR>'
    .&tab('/TD')
    .&tab('/TR')
    ."\n<!-- End of results for $alias -->\n";    

    $no_html = &set_flag;
    $result_collection_mail .= "${break}"
    .&tab('/TD')
    .&tab('/TR');    
    $no_html = 0;
}

# print index:
$tmp_message_len += 2; # make sure all former temporary messages 
                       # will be overwritten
#sysseek(OUT,"-$tmp_message_len",2);
$indent = 6;
$prev_indent = '-';
&sys_print('OUT',"\n"
       .&tab('TR')
       .&tab('TD')
       ."<BR>$date_range_msg<H4>Index of PubCrawler results:</H4><UL>");

$no_html = &set_flag;
$mail_results_file .= &tab('TR')
       .&tab('TD')
       ."${break}$date_range_msg_mail${h4In}Index of PubCrawler results:${h4Out}${ulIn}\n";
$no_html = 0;

$total_hits = 0;
foreach $alias (@alias_order) {
    my $hits = $hits{$alias};
    my $hits_mail = $hits{$alias};
    $total_hits += $hits;
    if ($hits == 1) {
    $hits_mail = "${boldIn}$hits${boldOut} new hit";
    $hits = "<B>$hits</B> new hit";
    } else {
    if ($hits == 0) {
        $hits_mail = 'no new hits';
        $hits = 'no new hits';
    } else {
        $hits_mail = "${boldIn}$hits${boldOut} new hits";
        $hits = "<B>$hits</B> new hits";
    }
    }
        # number of hits for each alias
    $alias =~ s/\"/&quot;/g; 
    my $alias_link = "<A HREF=\"#$alias\">$alias</A>";
    &sys_print('OUT',"\n<LI>$alias_link: $hits today<BR></LI>");
    unless (defined $mail_features{'html'}) {
        $alias_link = $alias;
    }
    $mail_results_file .= "${liIn}$alias_link: $hits_mail today${break}${liOut}";
}

&sys_print('OUT',"\n</UL><BR>"
       ."\n<CENTER><A HREF=\"#retrieval\">[retrieval]</A> <A HREF=\"#query_box\">[query box]</A> <A HREF=\"#disclaimer\">[disclaimer and copyright]</A></CENTER><BR>"
       .&tab('/TD')
       .&tab('/TR'));


if ($mail_features{'html'}) {
    $mail_results_file .=  "${ulOut}${break}"
       ."<CENTER>"
           .($mail_features{'javascript'} ? "<A HREF=\"#retrieval\">[retrieval]</A> <A HREF=\"#query_box\">[query box]</A> " : '')
           ."<A HREF=\"#disclaimer\">[disclaimer and copyright]</A></CENTER><BR>"
       .&tab('/TD')
       .&tab('/TR');
}


$trailer = &trailer; #make trailer (bottom of HTML output page) 
                     #with config file appended if requested

my $trailer_mail = &trailer('mail'); #make trailer (bottom of HTML output page) 
                     #with config file appended if requested

#print rest of document:
&sys_print('OUT',$result_collection.$trailer);
close OUT;

$mail_results_file .=  $result_collection_mail.$trailer_mail;
unless ($mail_features{'text'}) {
    my $mail_scripting = $ncbi_buttons."\n</FORM>\n<form method=\"post\" action=\"\" name=\"gotoNCBI\"></form>\n";
    my $mail_scripting_start = "\n$javascript\n<FORM NAME=\"myForm\" ACTION=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi$PARAM{'extra_query'}\" method=\"POST\">";
    unless ($mail_features{'javascript'}) {
        $mail_scripting = '';
        $mail_scripting_start = '';
    }
    my $mail_header = $html_header_mail."\n$mail_scripting_start\n<TABLE>\n";
    my $mail_ending = "\n</TABLE>\n$mail_scripting\n</BODY>\n</HTML>";
    unless ($mail_features{'html'}) {
        $mail_header = '';
        $mail_ending = '';
    }
    $mail_results_file = $mail_header.$full_records_mail.$mail_ending;
}

# set permissions for output file
if ($PARAM{'system'} =~ /macos/i) {
  MacPerl::SetFileInfo('MOSS','TEXT',"$PARAM{'log_file'}","$PARAM{'html_file'}");
}

if ($PARAM{'system'} =~ /unix/i){
    # make it readable (in case umask is messed up)
    chmod (0644,"$PARAM{'html_file'}");
}

if ($PARAM{'notify'} or $PARAM{'mail'} or $PARAM{'mail_ascii'} or $PARAM{'mail_simple'}) {
    if ($total_hits > 0) {
    print "\nSending mail, total hits: $total_hits\n";
    &mail_service;
    } elsif ($PARAM{'force_mail'} or $net_failure) {
    print "\nForcing mail, force_mail: $PARAM{'force_mail'}, net_failure: $net_failure, total hits: $total_hits\n";
    &mail_service;
    } else {
    print "\nNo mail sent, total hits: $total_hits\n";
    }
}

######################################
##########  save database  ###########
######################################
# save updated database only after mail was sent
&save_db;

print "Finished.\n";
print STDERR "Finished.\n" unless ($PARAM{'verbose'} or $PARAM{'mute'});

close LOGFILE;

# delete any temporary files
if (-e $tmp_file) {
    sleep 5;
    unlink $tmp_file;
}
# delete any temporary files
if (-e $tmp_file.'.mail') {
    sleep 5;
    unlink $tmp_file.'.mail';
}

if (-e $PARAM{'html_file'}.'.simple.mail') {
    sleep 5;
    unlink $PARAM{'html_file'}.'.simple.mail';
}

exit($EXIT_SUCCESS);

##########################################################################
################################# END OF MAIN ############################
##########################################################################


##########################################################################
################################# SUBROUTINES ############################
##########################################################################

sub start_results_file {

    my $username = '';
    if ($PARAM{'id'}) {
    $username = "for $PARAM{'id'}";
    }

    #header and trailer: copy header info if a header file exists:
    $PARAM{'header'} = "$PARAM{'prefix'}.header" unless ($PARAM{'header'});
    if (-e $PARAM{'header'}) {
    open(HEADER,"$PARAM{'header'}") || 
        die "$prog_name ERROR: $PARAM{'header'} exists but cannot be opened.\n";
    while (<HEADER>) {
	    $_ =~ s/\$\$([^\$]+)\$\$/$PARAM{$1}/ge; # macro substitution
        &sys_print('OUT',$_);
        $mail_results_file .= $_;
    }
    close HEADER;
    print "\nwriting header data from $PARAM{'header'} to $PARAM{'html_file'}\n";
    } else {

    $html_header = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">
<HTML>
<HEAD>
$css
<META NAME=\"ROBOTS\" CONTENT=\"NOFOLLOW\">
<TITLE>PubCrawler Results $username ".&timestamp('txt')."</TITLE>
</HEAD>
<BODY BGCOLOR=\"#FFFFFF\">
\n
<table align=\"center\" WIDTH='640' cellpadding='0' cellspacing='0'>
<tr>
<td align='right' width=\"20\"><a href=\"http://www.$$PARAM{proj}.org\"><img border="0" src=\"$PARAM{'icon'}\"></a></td>
<td ALIGN=\"center\" valign=\"middle\">
<b><font face=\"Arial,Helvetica\" size=+2>&nbsp;$PARAM{'proj'} PubMed and Entrez Updates&nbsp;</font></b>
</td>
</tr>
<tr><td><a href=\"http://www.$$PARAM{proj}.org\">>>PlasmoDB Home</a></td></tr>
</table>

";


    &sys_print('OUT',$html_header);

    print "\nno header file; writing default header to $PARAM{'html_file'}\n";
    
    unless ($mail_features{'css'}) {
        $css = '';
    }
    
    unless ($mail_features{'css'}) {
        $html_header_mail = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">
<HTML>
<HEAD>
<META NAME=\"ROBOTS\" CONTENT=\"NOFOLLOW\">
<TITLE>PubCrawler Results $username ".&timestamp('txt')."</TITLE>
</HEAD>
<BODY BGCOLOR=\"#FFFFFF\">
\n";
    } else {
        $html_header_mail = $html_header;
    }

    $mail_results_file .= $html_header_mail;

    }
    
    unless (defined $mail_features{'html'}) {
    $mail_results_file = '';
    }
    
    &sys_print('OUT',$javascript);
    
    
    if ($create_mail_file
    and
    defined $mail_features{'javascript'}) {
        $mail_results_file .= $javascript;
    }

    # the rest of the text for the mailing file has to be 
    # dealt with at the end of this subroutine
    
    my $picture = '';
    my $picture_replacement = "
          <table cellpadding=\"0\" cellspacing=\"0\" class=\"pub2\" border=\"2\" width=\"110\">
           <tr>
            <td>
             <table cellpadding=\"0\" cellspacing=\"0\" class=\"pub\" border=\"1\" width=\"102\">
              <tr>
               <td>
                <div align=\"center\">
                 <p><A HREF=\"http://www.pubcrawler.ie\"><font color=\"#000063\" size=\"7\"><b>Pub</b><br>
                  </font><b><font color=\"#000000\" size=\"5\">Crawler</font></b></A></p>
                </div>
               </td>
              </tr>
             </table>
            </td>
           </tr>
          </table>
";
    
    if ($PARAM{'pic'}) {
       $picture = "<A HREF=\"http://www.pubcrawler.ie\"><IMG BORDER=0 SRC=\"$PARAM{'pic'}\" ALT=\"LOGO\"></A>";
    } else {
    $picture = $picture_replacement;
    }
    
    my %user_links = (#'FAQ', 'http://pubcrawler.gen.tcd.ie/pubcrawler_www_faq.html', 
            #  'News', 'http://pubcrawler.gen.tcd.ie/webservice_news.html', 
           #   'WWW-Service', 'http://pubcrawler.gen.tcd.ie/www.html',
           #   'Settings', "http://pubcrawler.gen.tcd.ie/cgi-bin/pubcrawler_www2.pl?submit=Log+in!&pc_user=$PARAM{'id'}",
#             'Queries', "http://pubcrawler.gen.tcd.ie/cgi-bin/pubcrawler_buttons?submit=Modify+Queries&pc_name=$PARAM{'id'}",
           #   'WWW-Service', 'http://pubcrawler.gen.tcd.ie/www.html',
          #    'Previous&nbsp;Results', "http://pubcrawler.gen.tcd.ie/cgi-bin/pubcrawler_buttons?submit=Previous+Results&pc_name=$PARAM{'id'}",
              );
    my  @user_links_order;# = ('FAQ', 'News');
    my $user_links = '';

    if ($PARAM{'id'}) {
#   push @user_links_order, ('Profile', 'Queries');
    push @user_links_order, ('Settings');
    }
    push @user_links_order, ('WWW-Service', 'Previous&nbsp;Results');
    foreach (@user_links_order) {
    my $link = $user_links{$_} or next;
    $link = "<a href=\"$link\">$_</a>";
    $user_links .= "\n<p><font color=\"#ffffff\">|__</font>&nbsp;$link</p>\n";
    }
    
    my $extra_links = "
<P><BR><CENTER><a href=\"http://biology.plosjournals.org/\" TARGET=viewer>
<img SRC=\"http://pubcrawler.gen.tcd.ie/pics/plos.jpg\" ALT=\"PLOS\"></a>
</CENTER>
</P>
";

    my $extra_links_no_pic = "
<P><BR><CENTER><a href=\"http://biology.plosjournals.org/\" TARGET=viewer>PLOS</a>
</CENTER>
</P>
";
    my $extra_links_no_html = "PLOS - we are open (http://biology.plosjournals.org)\n";

$extra_links = '';
$extra_links_no_pic = '';
$extra_links_no_html = '';

    ($dateline, $timestamp) = &timestamp;           
    
    &sys_print('OUT',"
<table border=\"0\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" width=\"640\">
   <tr>
<!--
left column
-->

<!--
main column
-->
   <td>
    <FORM NAME=\"myForm\" ACTION=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi$PARAM{'extra_query'}\" method=\"POST\">
     <div>
      <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">
       <tr>
        <td bgcolor=\"$PARAM{'bg'}\">
         <div align=\"right\">
          <h3><b>$dateline&nbsp;<font color=\"#ffffff\">::::::</font></b></h3>
         </div>
        </td>

       </tr>"); #DON'T remove blanks! (needed as space holders for temp-message)
    $indent = 6;
    $prev_indent = '-';
    
    
    # fill up a buffer with whitespaces 
    # to avoid leftovers of partially erased temp messages
    my $buffer = ' ' x (2 * $#query_order);
    $tmp_message =  " \n$buffer\n<H1>Temporary PubCrawler Results File</H1>
Execution of your PubCrawler jobs is currently in progress (started at <B>".(localtime)."</B>) ... please check again later!<BR>So far: approx. $update_message";
    $tmp_message_len = length $tmp_message; 
    #&sys_print('OUT',$tmp_message);
    

    # assemble output for mail file
    if ($create_mail_file) {
    $user_links = '';
    
    my $form = "<FORM NAME=\"myForm\" ACTION=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi$PARAM{'extra_query'}\" method=\"POST\">";
    unless ($mail_features{'javascript'}) {
        $form = '';
    }

    $picture = "<A HREF=\"http://www.pubcrawler.ie\"><IMG BORDER=0 SRC=\"$PARAM{'pic'}\" ALT=\"LOGO\"></A>";
    unless ($mail_features{'images'}) {
        $picture = $picture_replacement;
        unless ($mail_features{'html'}) {
        $picture = '';
        }
    }

    if ($mail_features{'pubcrawler_links'}) {
        foreach (@user_links_order) {
        my $link1 = $user_links{$_} or next;
        $link1 = "<a href=\"$link1\">$_</a>";
        if ($mail_features{'html'}) {
            $user_links .= "\n<p><font color=\"#ffffff\">|__</font>&nbsp;$link1</p>\n";
        } else {        
            $user_links .= "\n|__ $_ ($user_links{$_})\n";
        }
        }

        unless ($mail_features{'html'}) {
        $picture = "\nPubCrawler\nIt goes to the library  - you go to the pub(TM)\n";
        }
    } else {
        $picture = "\nPubCrawler\nIt goes to the library  - you go to the pub(TM)\n";
    }

    if ($mail_features{'html'}) {
        if ($mail_features{'pubcrawler_links'}) {
        $mail_results_file .= "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"706\">
   <tr>
<!--
left column
-->
    <td width=\"155\" bgcolor=\"#EFEFEF\" valign=\"top\">    
     <div align=\"center\">

     <b><big>PubCrawler Results $username</big></b><br>
      <div>
       <table border=\"0\" cellpadding=\"10\" cellspacing=\"0\" width=\"90%\">
        <tr>
         <td>
          <div align=\"center\">
          <br>
$picture
          <div class=\"small\">
           <font color=\"#000000\">

           
           &nbsp;<i>www.$PARAM{'proj'}.org</i></font></div>
          </div>
         </td>
        </tr>
        <tr>
         <td>
          <p><br>
         </p>

$user_links
$extra_links

        </td>

        </tr>
       </table>
    </td>

<!--
space holder
-->
    <td width=\"10\" bgcolor=\"white\" valign=\"top\">    
&nbsp;&nbsp;&nbsp;
    </td>
<!--
main column
-->
   <td width=\"445\">
     $form
     <div>
      <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"95%\">
       <tr>
        <td bgcolor=\"$PARAM{'bg'}\">
         <div align=\"right\">
          <h3><b>$dateline&nbsp;<font color=\"#ffffff\">::::::</font></b></h3>
         </div>
        </td>

       </tr>
";
        } else {
        $mail_results_file .= "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"600\">
   <tr>
    <td>
     $form
     <div align=\"center\">
      <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"95%\">
       <tr>
        <td bgcolor=\"$PARAM{'bg'}\">
         <div align=\"right\">
          <h3><b>$dateline&nbsp;<font color=\"#ffffff\">::::::</font></b></h3>
         </div>
        </td>

       </tr>";  
        }
    } else {
        my $dateline_txt = &timestamp('txt');
        $mail_results_file .= "${form}PubCrawler Results $username on $dateline_txt  ::::::
$picture$user_links$extra_links";
        
    }
    }
}


sub log_query {
    my $time_1 = shift;
    my $type = shift;
    my $out = shift;
    my $in = shift;
    my $time = time;
    my $diff = $time - $time_1;
    system "echo \"$time_1 $diff $type $out $in $log_id\" >> $query_log";
}

sub log_mail {
    my $time_1 = shift;
    my $type = shift;
    my $len = shift;
    my $time = time;
    my $diff = $time - $time_1;
    system "echo \"$time_1 $diff $type $log_id $len\" >> $mail_log";
}

sub sys_print {
    # subroutine which prints to files without buffering
    
    my $fh = shift;
    my $message = shift;
    my $len = length $message;

    my $written = syswrite $fh,$message,$len;

    unless ($written == $len) {
    die "Couldn't complete writing of $message $PARAM{'id'} to filehandle $fh: $!";
    }
}
    

sub compile_notification {
    # Since this feature is mainly intended
    # for the PubCrawler WWW-Service
    # I decided to send an HTML-Notification
    # CHANGED to more flexible setup (see -mail_features option, Karsten Hokamp, Feb 2004)
    
    my $address = shift;
    my $subject = shift;
    my $nick_name = '';
    my $for_name = '';
    my $note_link = '';

    # the name of the user might be appended
    # to the address (separated by '#' or '@@')
    if ($PARAM{'notify'} =~ /#/) {
    ($PARAM{'notify'}, $nick_name) = split /#/, $PARAM{'notify'};
    } elsif ($PARAM{'notify'} =~ /@@/) {
    ($PARAM{'notify'}, $nick_name) = split /@@/, $PARAM{'notify'};
    }

    if ($nick_name) {
    $nick_name =~ s/\"$//;
    $for_name = "for $nick_name";
    $note_link = "<A HREF=\"$link_gen/db/$nick_name\">$link_gen/db/$nick_name</A>";
    unless ($mail_features{'html'}) {       
        $note_link = "$link_gen/db/$nick_name";
    }
    } else {
    $for_name = '';
    $note_link = "${boldIn}$link_gen/db/${itIn}user_name${itOut}${boldOut} (replace 'user_name' with the name you are registered with)";
    }
    
    # write the message body to 
    # a temporary file...
       
    # get localtime 
    my $date = localtime;
    $date .= " $timezone"; #(Greenwich Mean Time or Irish Summer Time)
    
    my $total_hits = "${boldIn}$total_hits${boldOut}";
    
    if ($PARAM{'force_mail'} and $total_hits eq "${boldIn}0${boldOut}" ) {
    $total_hits .= " ${boldIn}(mail forced)${boldOut}";
    }
    if ($net_failure) {
    $total_hits .= " ${boldIn}Possible network failure!${boldOut}";
    }
    
    my $out_file = $PARAM{'html_file'}.'.mail';
    open (NOTE, ">$out_file")
    or die "Can't write mail body to $out_file: $!\n";
    if ($mail_features{'html'}) {   
    print NOTE "<HTML><HEAD><TITLE>PubCrawler Notification</TITLE></HEAD>
    <BODY BGCOLOR=#FFFFFF>
    <H1>PubCrawler Notification $for_name</H1>
    Your PubCrawler job finished at $date.
    <BR>
    Number of new hits: $total_hits
    <BR>
    <BR>\n";
    } else {

    my $address_clean = $address;
    ($address_clean) = split /\#/, $address if ($address =~ /\#/);
    ($address_clean) = split /\@\@/, $address if ($address =~ /\@\@/);

    $address_clean = '"'.$address_clean.'"';
    $address_clean =~ s/\"\"/\"/g;

    # we might have to escape single ticks unless it's done already
    $address_clean =~ s/([^\\])'/$1\\'/g;

    print NOTE "From: $sender\nTo: $address_clean\nSubject: $subject\n\n";
    print NOTE "PubCrawler Notification $for_name

Your PubCrawler job finished at $date.

Number of new hits: $total_hits\n\n";
    }

    print NOTE "Your results have been written to $note_link .";

    # the next string in particular is only
    # useful for people registered with WWW-PubCrawler
    if ($mail_features{'pubcrawler_links'}) {   
    my $link = "<a href=\"http://pubcrawler.gen.tcd.ie/cgi-bin/pubcrawler.results\">http://pubcrawler.gen.tcd.ie/cgi-bin/pubcrawler.results</a>";
    unless ($mail_features{'html'}) {
        $link = 'http://pubcrawler.gen.tcd.ie/cgi-bin/pubcrawler.results';
    }
    print NOTE "${break}${break}If you have ${boldIn}'Easy Check'${boldOut} activated, you can see PubCrawler's output at $link.";
    $link = "<a href=\"http://pubcrawler.gen.tcd.ie/pubcrawler_www_faq.html\">FAQ</A>";
    unless ($mail_features{'html'}) {
        $link = 'FAQ at http://pubcrawler.gen.tcd.ie/pubcrawler_www_faq.html';
    }
    print NOTE "${break}${break}${boldIn}NEW:${boldOut} For answers to frequently asked questions check out the $link!\n";
    } 

    print NOTE "${break}${break}Have a nice day!\n";
    
    if ($mail_features{'html'}) {   
    # finish HTML-document
    print NOTE "\n</BODY></HTML>\n";
    }
    
    close NOTE;
    
    return $out_file;
}


sub mail_service {

    # this will not work on every system 
    # it is also tailored for the PubCrawler WWW-Service
    # people trying to use this feature
    # will have to adjust this subroutine

        # get day and month for mail subject
    my $subj_date = &timestamp('mail');
    my $total_hits_copy = $total_hits;

    my $check_string = '';
    if ($PARAM{'check'}) {
    $check_string = ' (CHECK)';
    }

    my $address = '';
    my $tag = '';
    my $mail_file = '';

    my $mail_only = 0;
    if ($PARAM{'mail_only'} =~ /\d/) {
    $mail_only = 1;
    $mail_file = $PARAM{'html_file'}.'.mail';
    unless (-e $mail_file) {
        $mail_file = $PARAM{'html_file'};
    }
    }

    # generate a string holding 
    # subject of mail and address
    my $subject = "PubCrawler Results, $subj_date$check_string";

    if ($PARAM{'notify'}) {

    $address = $PARAM{'notify'};
    $tag = 'note';

    $subject = "PubCrawler Notification, $subj_date$check_string";

    $mail_file = &compile_notification($address,$subject) unless ($mail_only);

    } elsif ($PARAM{'mail_ascii'}) {
    $address = $PARAM{'mail_ascii'};
    $tag = 'ascii';
    } elsif ($PARAM{'mail_simple'}) {
    $address = $PARAM{'mail_simple'};
    $tag = 'simple';
    } elsif ($PARAM{'mail'}) {
    $address = $PARAM{'mail'};
    $tag = 'full';
    }

    if ($address) {
    # mail the whole output file
    # as an ascii file
    # do the conversion from HTML to ascii first
    # and then proceed as with the normal mail function
    
    my $type = 'text/ascii';
    if ($mail_features{'html'}) {   
        $type = 'text/html';
    }

    if ($copy_mail_file) {
        # all mail features activated
        # send the normal output file
        $mail_file = $PARAM{'html_file'};
    } else {
        unless ($tag eq 'note'
            or
            $mail_only
            ) {
        # produce mail file
        $mail_file = $PARAM{'html_file'}.'.mail';
        unless (open (OUT, ">$mail_file")) {
            warn "Can't write mail to $mail_file: $!\n";
            $mail_file = $PARAM{'html_file'};
        } else {
            my $address_clean = $address;
            ($address_clean) = split /\#/, $address if ($address =~ /\#/);
            ($address_clean) = split /\@\@/, $address if ($address =~ /\@\@/);

            $address_clean = '"'.$address_clean.'"';
            $address_clean =~ s/\"\"/\"/g;
            
            # we might have to escape single ticks unless it's done already
            $address_clean =~ s/([^\\])'/$1\\'/g;
            
            print OUT "From: $sender\nTo: $address_clean\nSubject: $subject\n\n" if ($type =~ /ascii/);
            print OUT $mail_results_file;
            close OUT;
        }
        }
    }       

    $mail_len = &mail_results($mail_file,
                  $address,
                  " -m $type", 
                  $subject);
    if ($PARAM{'log_mail'}) {
        &log_mail($time_1,$tag,$mail_len) unless ($PARAM{'mail_relay'});
    }
   }


    if ($PARAM{'mail_relay'}) {
    system "echo $total_hits_copy >> $PARAM{'mail_relay'}";
    $PARAM{'mail_relay'} =~ s/(.*)\.(.*)/$1/;
    system "mv $PARAM{'mail_relay'}.$2 $PARAM{'mail_relay'}";
    return;
    }
}


sub mail_results {
    my $file = shift;
    my $address = shift;
    my $meta = shift;
    my $subject = shift;
    my $nick_name;
    my $for_name;
    my $err_txt = '';
    my $size = 0;

    if ($address =~ /#/) {
    ($address, $nick_name) = split /#/, $address;
    } elsif ($address =~ /@@/) {
    ($address, $nick_name) = split /@@/, $address;
    }
    
    $address = '"'.$address.'"';
    $address =~ s/\"\"/\"/g;

    # we might have to escape single ticks unless it's done already
    $address =~ s/([^\\])'/$1\\'/g;

    if ($nick_name) {
    $nick_name =~ s/\"$//;    
    $for_name = "for $nick_name";
    } else {
    $for_name = '';
    }

    unless ( -s "$file") {
        # results file is empty
        # send error message
    $err_txt = "<HTML><HEAD><TITLE>PubCrawler Mailing Service</TITLE></HEAD>\n<BODY BGCOLOR=#FFFFFF>\n<H1>PubCrawler Mailing Service $for_name</H1>\n";
    $err_txt .= "\nSorry, but your results file '$file' was empty.\n<BR>\nAn error must have occured during the execution of PubCrawler.\n";
    $err_txt .= "<BR><BR>\nYou should check your result at http://pubcrawler.gen.tcd.ie/db/${for_name} and consider restarting them manually.\n";
    $err_txt .= "<BR><BR>\nPlease inform $sender if this event should reoccur!\n";
        # finish HTML-document
    $err_txt .= "\n</BODY></HTML>\n";

        # strip off HTML-tags, if ascii-results requested
    if ($meta =~ /ascii/) {
        $err_txt =~ s/<[^>]*>//sg;
    }

        # write message to file
    open (MAIL, ">$tmp_file")
        or die "Can't write mail body to $tmp_file: $!\n";

    if ($meta =~ /ascii/) {
        print MAIL "From: $sender\nTo: $address\nSubject: $subject (ERROR)\n\n";
    }
    print MAIL $err_txt;
    close MAIL;

    $file = $tmp_file;
    }
    
    # send mail!
    $mail_prog .= "$meta -s '$subject' -t $address -f $file";
    if ($meta =~ /ascii/) {
    $mail_prog = "$sendmail $address < $file";
    }
    $time_1 = time if ($PARAM{'log_mail'});
    system "$mail_prog" unless($PARAM{'mail_relay'});
    $size = (stat $file)[7] + length($subject) if ($PARAM{'log_mail'});
    return $size;
}


#  FIRST_VISIT
sub first_visit{ 
#first visit to NCBI to get list of UIDs matching query
# called from MAIN
# (this sub is called once for each query)
# requires $PARAM{'getmax'}(global), $PARAM{'relentrezdate'}(global), database, query-string
#initialise:
    my $search_type = shift; # database for query
    my $query = shift;       # query-string
    my $query_URL = '';
    my @uids = ();  # list that holds the uids
    my @tmp = ();   # temporary array
    my $query_error_msg = '';
    my $uid_result = '';
    my $docstring = '';  
    my $hits = '';
    my $term = '';
    my $connection_error = '';
    my $retry_local = 0;

    #remove comments from query
    ($query) = split /#/, $query;
    my $query_orig = $query;
    
    my $add_reldate = 1;
    my $neighbour_search = 0;

    # special treatment of relation-requests:
    if ($search_type =~ /rel(\w)/) {
    $search_type = $1;
    $neighbour_search = 1;
    my $from_db = 'pubmed';
    my $to_db = 'pubmed';

    if ($search_type eq 'n') {
        $add_reldate = 0;
        # neighbourhood search for GenBank not supported anymore
        my $msg_link = "$link_gen/webservice_news.html";
#       return (0,"Neighbourhood search for GenBank <B>currently disabled</B>, please see <A HREF=\"$msg_link\">WWWW-Service News</A> for details!",,);
        $from_db = 'nucleotide';
        $to_db = 'nucleotide';
    }
    $query =~ s/\+//g;
    $term = "id=$query";
    
    $query_URL = $PARAM{'neighbour_URL'};
        # assemble docstring:
    $docstring = join '&', ("dbfrom=$from_db",
                "db=$to_db",
                'cmd=neighbor',
                'usehistory=n',
                'mode=xml',
#               'retmax='.$PARAM{'getmax'}, # doesn't work!
                "tool=$PARAM{'tool'}",
                "email=$sender",
                "$term"
                );
    } else {

    $term = "term=$query";
    # author name does not work at the nucleotide db
    $term =~ s/\[author\+name\]/\[author\]/gi;

    $query_URL = $PARAM{'search_URL'};
    # assemble docstring:
    $docstring = join '&', ("db=$search_db{$search_type}",
                'usehistory=n',
                'mode=xml',
                'retmax='.$PARAM{'getmax'},
                "tool=$PARAM{'tool'}",
                "email=$sender",
                "$term");
    }

    
    if ($date_range) {
    my $tmp_date_range = $date_range;
    $tmp_date_range =~ s/XXX/$date_range{$search_type}/g;
    $docstring .= $tmp_date_range;
    if ($search_type eq 'n') {
        $docstring .= '&datetype=mdat';
    } else {
        $docstring .= '&datetype=edat';
    }
    } else {
    if ($PARAM{'relentrezdate'} < 14408   #current limit (26/02/2000)
        and
        $add_reldate) { 
        # add relentrezdate or relmoddate, depending on database
        $docstring .= '&'.$date_limit{$search_type}."=$PARAM{'relentrezdate'}";
        if ($search_type eq 'n') {
        $docstring .= '&datetype=mdat';
        } else {
        $docstring .= '&datetype=edat';
        }
        # (leaving out date limit will act like 'no limit')
    }
    }
    
    
    print "\n===\n$query{$query_orig}{'ALIAS'}:\nmaking HTTP connection to:\n$query_URL?$docstring\n";
    
    # get the results of the HTTP-request
    # requires: $PARAM{'search_URL'}(global), $docstring   
    # produces: $uid_result
    $time_1 = time if ($PARAM{'log_queries'});
    ($uid_result,$connection_error) = &make_HTTP($query_URL,$docstring);
        # log query and traffic
    if ($PARAM{'log_queries'}) {
    if ($PARAM{'from_quick'}) {
        &log_query($time_1,3,length($docstring),length($uid_result));
    } else {
        &log_query($time_1,1,length($docstring),length($uid_result));
    }
    }

    my $entrez_error = '';
    if ($uid_result =~ /<error>(.*)<\/error>/si) {
    $entrez_error = $1;
    if ($uid_result =~ /<errorlist>(.*)<\/errorlist>/si) {
        my $tmp_entrez_error = $1;
        if ($tmp_entrez_error =~ /<PhraseNotFound>/) {
        $entrez_error = 'At least one of the phrases was not found.';
        }
    }
    } 
    # retry in case Server Error occured
    while ( ( $uid_result =~ /Server Error/s
          or
          $connection_error
          or
          $uid_result !~ /<idlist>/si
         )
        and
        $retry_local < $PARAM{'retry'}
        and
        $entrez_error eq ''
       ) {
    my $text = ($connection_error ?
            'Connection error' :
            'Server error');
    $retry_local++;
    my $time_now = localtime;
    print "\nWARNING at $time_now: $text, starting retry $retry_local...\n";
    warn "WARNING at $time_now $PARAM{'id'}: $text, retry $retry_local for query $query at first visit\n" if ($retry_local > 3);
        print "sleeping for $PARAM{'break'} seconds...\n";
        sleep($PARAM{'break'}); 
    $time_1 = time if ($PARAM{'log_queries'});
    ($uid_result,$connection_error) = &make_HTTP($query_URL,$docstring);
            # log query and traffic
    if ($PARAM{'log_queries'}) {
        if ($PARAM{'from_quick'}) {
        &log_query($time_1,'3.'.$retry_local,length($docstring),length($uid_result));
        } else {
        &log_query($time_1,'1.'.$retry_local,length($docstring),length($uid_result));
        }
    }
    }
    
    # write into output file how much has been done
#    &update_output;

    my $results_num = 0;
    my $uid_error = 0;
    my $target = 'idlist';
    if ($neighbour_search) {
    $target = 'linksetdb';
    }
    if ($uid_result =~ /<$target>(.*)<\/$target>/si) {
    $uid_result = $1;
    } else {
    $uid_error = 1;
    }

    my $uid_counter = 0;
    unless ($uid_error) {
    @tmp = ();
    while ($uid_result =~ /<id>/si) {
        $uid_result =~ s/<id>(.*?)<\/id>//si;

        # since neighbour searches can not be shortened
        # to a max number of results
        # we have to do it here:
        $uid_counter++;
        if ($uid_counter > $PARAM{'getmax'}) {
        last;
        }
        push @tmp, $1;
    }
    $uid_result = join ' ', @tmp;
    $results_num = @tmp;
    }

    if ($uid_result =~ /<OutputMessage>(.*)<\/OutputMessage>/is) {
    $uid_result = $1;
    }

    if ($entrez_error) {
    $uid_result = $entrez_error;
    }

   # remove HTML tags from retrieved list of UIDs 
    $uid_result =~ s/.*<body>//igs;
    $uid_result =~ s/<.*?>//sg;
    print "result of query is:\n$uid_result\n$results_num hits\n";
    
    #check for text (error message) instead of UID numbers in result:    
    if ($uid_result =~ /Can not find neighbor/s) {
    my $msg_link = "$link_gen/neighbour_help.html";
    $hits = 0;
    $query_error_msg = "No neighbours found - please make sure you are using a PMID and not a Medline UI (see <A HREF=\"$msg_link\">Neighbour Help</A> for details!";
    print "No neighbours found - possible use of UID instead of PMID!\n\n";
    } elsif ($uid_result !~ /\d/s
    or $uid_result =~ /Server Error/s   
    or $connection_error){
    $hits = 0;  
    if ($uid_result =~ /[a-zA-Z]/s) {
        $query_error_msg = "'$uid_result' (from search: $query{$query}{'ORIG'})<br>";
        print "\ngot TEXT instead of UIDs from the query.\n\n";
    }
    } else {
    @tmp = split(/\s+/,$uid_result);   # @tmp is the list of 
                              # matching UIDs for this query
    foreach (@tmp) {
        # drop everything that doesn't contain a digit
        if (/\d/) {
        push @uids, $_;
        }
    }
    $hits = $#uids + 1; 
#   print "UIDs from this query are: @uids \n\n";
    }
    return($hits,$query_error_msg,@uids);
}#return from sub first_visit
    

sub format_record {
    my $search_db = shift;
    my $format = shift;
    my $record = shift;
    my $num = shift;
    my $html_form = shift || '';
    my $html_formatting = shift || '';
    my $add_link = shift || '';
    my %record = ();
    my $out = '';

    if ($format =~ /xml/i) {
    $out = $record;
    if ($html_formatting eq 'website'
        or
        $mail_features{'html'}) {
        $out .= '<BR><BR>';
    }
    return $out;
    }
    
    foreach my $line (split /\n/, $record) {
    if ($line =~ /<Item Name="(.*?)" Type.*?>(.*)<\/Item>/) {
        push @{$record{$1}}, $2;
    } elsif ($line =~ /<Id>(.*)<\/Id>/) {
        push @{$record{'PubMedId'}}, $1;
    }
    }

    my @out = ();

    my $ID = '';
    # make sure that there is at least a space
    my $related_items = '&nbsp;';

    # PubMed specifics
    if ($search_db eq 'pubmed') {

    # first line:   
    unless (defined @{$record{'Author'}}) {     
        $out[0] = '[No authors listed]';
    } else {
        $out[0] = join ', ', @{$record{'Author'}};
        $out[0] .= '.';
    }

    $ID = $record{'PubMedId'}[0] || '[No ID listed]';   

    # Publication info varies depending on which fields are available:
    my $pub_info = '';
    if (defined $record{'Lang'}[0]
        and
        $record{'Lang'}[0] ne 'English') {
        $pub_info = ' '.$record{'Lang'}[0].'.';
    } elsif (defined $record{'PubStatus'}[0]
         and
         $record{'PubStatus'}[0] eq 'aheadofprint') {
        $pub_info = " [Epub ahead of print]";
    } elsif (defined $record{'PubType'}[0]
         and
         $record{'PubType'}[0] eq 'Review') {
        $pub_info = ' '.$record{'PubType'}[0].'.';
    }

    $out[1] = $record{'Title'}[0] || '[No title listed]';
    
    $out[2] = "$record{'Source'}[0]. $record{'SO'}[0].$pub_info";
    $out[3] = "PMID: $ID \[$record{'RecordStatus'}[0]]";

    if ($format =~ /brief/i) {

        my @tmp = ($out[0]);

        # reduce authors names to 1
        if (defined $record{'Author'}) {
        if (@{$record{'Author'}} > 1) {
            $tmp[0] = $record{'Author'}[0] . ' et al';  
        }
        $tmp[0] .= '.';
        } else {
#       warn "No author for $out[0]";
        sleep 0;
        }
        
        # shorten title
        $tmp[1] = $out[1];
        $tmp[1] =~ s/(.{29}).*/$1\.\.\./;
        
        $tmp[2] = "[PMID: $ID]";

        @out = @tmp;
    } 

    if ($mail_features{'entrez_links'}
        or
        $html_formatting eq 'website') {
        $related_items = "&nbsp;&nbsp;<A HREF=\"$related_article_link$ID\"><SMALL>Related&nbsp;Articles</SMALL></A>";
        unless ($mail_features{'html'}) {
        $related_items = " Related Articles: $related_article_link$ID" unless ($html_formatting eq 'website');
        }
    }

    # GenBank specifics
    } elsif ($search_db eq 'nucleotide') {

    $out[0] = $record{'Caption'}[0] || '[No Caption listed]';
    $ID = $record{'Gi'}[0] || '[No Gi listed]';
    $out[1] = $record{'Title'}[0] || '[No Title listed]';
    $out[2] = $record{'Extra'}[0].'['.$record{'Gi'}[0].']';

    if ($format =~ /brief/i) {

        my @tmp = ($out[0]);

        # shorten title
        $tmp[1] = $out[1];
        $tmp[1] =~ s/(.{17}).*/$1\.\.\./;
        
        $tmp[2] = "[gi:$ID]";

        @out = @tmp;
    } 

    } else {
    return "ERROR: unknown database: '$search_db'\n";
    }

    if ($add_link eq 'website') {
    $out[0] = "<A HREF=\"$abstract_link{$search_db}$ID\">$out[0]</A>";
    } else {
    if ($mail_features{'entrez_links'}) {
        if ($mail_features{'html'}) {
        $out[0] = "<A HREF=\"$abstract_link{$search_db}$ID\">$out[0]</A>";
        } else {
        push @out, "Link: $abstract_link{$search_db}$ID";
        }
    }
    }

    my $counter = "$num: ";
    unless ($mail_features{'text'}) {
    $counter = '' unless ($add_link eq 'website');
    }

    my ($font_up,$font_down,$font_end);
    my $line_break = "\n";
    if ($html_form) {
    $counter = "$html_form\"$ID\">$counter";
    }

    if ($format =~ /brief/i) {
    $out[0] .= '.' if ($search_db eq 'nucleotide');
    $out[0] .= ' ';
    my $brief = join '', @out;
    @out = ($brief);
    }

    my $end = "\n";
    if ($html_formatting) {
    $counter = &tab('TR').
        &tab('TD',' WIDTH="49" NOWRAP VALIGN=TOP ALIGN=LEFT')
        ."<B>$counter&nbsp;</B>"
        .&tab('/TD')
        .&tab('TD',' ALIGN=LEFT');
#   $font_up = '<font size="+1">';
#   $font_down = '<font size="-1">';
#   $font_end = '</font>';
#   $font_down = '<small>';
#   $font_end = '</small>';
    $out[0] = 
        $out[0]
        .&tab('/TD')
        .&tab('TD', ' VALIGN=TOP ALIGN=RIGHT')
        .$related_items;

    $line_break = 
        &tab('/TD')
        .&tab('/TR') 
        .&tab('TR')
        .&tab('TD')
        .'&nbsp;'
        .&tab('/TD')
        .&tab('TD', ' COLSPAN=2');
    $end = 
        "<BR><BR>"
        .&tab('/TD')
        .&tab('/TR');
    } else {
    $counter = "\n".$counter;
    $related_items =~ s/\&nbsp;//g;
    push @out, $related_items if ($related_items);
    }
    $out[0] = $counter.$out[0];

    $out = join "$line_break", @out;
    $out .= $end;
    #$out .= "$counter$out[0]\n$record{'Title'}[0]\n${line_break}\n$font_down$record{'Source'}[0]. $record{'SO'}[0].$pub_info\n${line_break}PMID: $ID \[$record{'RecordStatus'}[0]]$font_end\n$end";

    return $out;
}
    
##########################################################################
#  LIST_CRUNCH
#subroutine to compare the UIDs (returned from a first search) 
#to the databases and retain only the new-ish ones
#called from MAIN
sub list_crunch{
    my $alias = shift;
    my @uid_list = @_;
    my $uid;
    my @list_shrunk=();
    my $prev;
    my $lifespan;
    my $forget;
    my $now;
    my @goget=();
    my $delete_forget = 1;
    my $sort_by_age = 1;
    my %done = ();

    # check if query is a neighbourhoodsearch
    # in which case the previous hits should be kept:
    foreach my $query ( @{ $aliases{$alias} } ) {
    if ($query{$query}{'DB'} =~ /rel/
        or
        $query{$query}{'DB'} =~ /neighbour/) {
        $delete_forget = 0;
        $sort_by_age = 0;
        last;
    }
    }
    
#strip extra characters from UIDlist
    foreach (@uid_list) { s/\s//g; s/\n//g; }
    
#cumbersome way to remove duplicates; 
#list_shrunk has no duplicate entries
#    $prev="";
#    foreach (reverse sort(@uid_list)) {
#   unless ($_ eq $prev) {push(@list_shrunk,$_);}
#   $prev=$_;
#    }

    # changed this to avoid sorting, which would mess up order of neighbourhood results
    foreach (@uid_list) { 
    unless (defined $done{$_}) {
        $done{$_}++;
        push @list_shrunk, $_;
    }
    }
    @uid_list=@list_shrunk;
    undef @list_shrunk;
    
    if ($date_range) {

    print " - skipped because of date range!\n";
    # specific date was set - don't filter old articles
    $alias_global = $alias;    
    $now = sprintf "%.2f",(time/86400); # round value ...
    foreach (@uid_list) {
        ${ $db{$alias} }{$_} = $now;
        $age{$_} = 0;       
    }
    @goget = sort by_time_first_seen_and_uid @uid_list;
    return(@goget);
    }

# Log file contains UIDs and the time (Unix, Win, or Mac time stamp) 
# they were first seen.
# Can forget old log entries when they are older than $PARAM{'relentrezdate'}.
# The factor +3 is to guard against any time/date differences
# as compared to NCBI's RELENTREZDATE clock
# (need to keep entries in the database for a little longer 
# than RELENTREZDATE days)
    
    $lifespan=$PARAM{'viewdays'} +3;  #maximum age (in days) for a record 
                                      #to be displayed
    $forget=$PARAM{'relentrezdate'} +3;  #age above which a record can be 
                                      #deleted from the database
#    $now=time/86400;      #unix and Macintosh time baselines are different 
                          #but both work in seconds
    $now = sprintf "%.2f",(time/86400); # round value ...
    print "now: $now; max age allowed (lifespan): $lifespan; ";
    print "age to forget: $forget \n";

# compare the UIDs in the search-results list to those in the database
# go-get them / ignore them / delete from database / 
# add to database as appropriate
# note: time_first_seen{} is local to this subroutine; age{} is global
    foreach $uid (@uid_list){
    next unless ($uid =~ /\d/);
    unless (${ $db{$alias} }{$uid}) { ${ $db{$alias} }{$uid} = $now; }
    $age{$uid} = $now - ${ $db{$alias} }{$uid};
    print "uid: $uid time_first_seen: ${ $db{$alias} }{$uid} age: $age{$uid} ";
    if ($age{$uid} <= $lifespan) {
        push(@goget,$uid);  #goget is the list of UIDs to get 
                            #on the 2nd visits
        print "\n";
        } else {
        print "is too old.\n";
        }
    if ($age{$uid} > $forget) {
        print "forgetting it.\n";
            #avoid huge database files
            # but keep for neighbourhood search
        if ($delete_forget) {
        delete ${ $db{$alias} }{$uid}; 
        } else {
        print "keeping entries for neighbourhood search ($alias, $query{$aliases{$alias}[0]}{DB})\n";
           }
    }
    }

    # sort by age unless we are dealing with 
    # specially sorted id's from neighbourhood search
    if ($sort_by_age) {
    $alias_global = $alias;    
    @goget = sort by_time_first_seen_and_uid @goget;
    }

    return(@goget);
} ## return from sub list_crunch



################################################################################
# SECOND_VISIT
# subroutine to make second visit to PubMed or Genbank
# requires: $alias $search_type database-name @query_list $PARAM{'fullmax'}(global) 
#           $PARAM{'viewdays'}(global) %age (from &list_crunch) $PARAM{'retrieve_URL'}(global
# returns: $second_visit_result
sub second_visit {
        
    my $alias = shift;
    my $search_type = shift;
    my $word = shift;
    my @query_list = @_;
    my $button;
    my $docstring;
    my $number_of_1day_records;
    my @query_list_1day;
    my $day;
    my $uidstring;
    my @query_big;
    my @query_tmp;
    my $counter;
    my $second_visit_result = '';
    my $second_visit_result_mail = '';
    my $dopt = $PARAM{'results_format'};
    my $dispmax = '';
    my $tmp_result;
    my $base_href;
    my $connection_error = '';
    my $retry_local = 0;
    my $advise = '';
    my $status = '';

#retrieve UIDs in $query_list in single-day age groups, 
#between age 0 and age $PARAM{'viewdays'}:

    for ($day=0; $day <= $PARAM{'viewdays'}; $day++){
        $button = '';
    @query_big = ();
    foreach (@query_list){
        if ($day == int(($age{$_} + 0.5))) { #0.5 term is to prevent age of
                                       #0.99 days being rounded down,etc
        push(@query_big,$_);
        }
    }
    # shorten query list if too long...
    if ($day == 0) {
            # assemble $PARAM{'fullmax'} of uids to uid string
        @query_list_1day = splice(@query_big, 0, $PARAM{'fullmax'});
        $dispmax = $PARAM{'fullmax'};
        $counter = $PARAM{'fullmax'} + 1;

        if ($simulation_file) {
        @query_list_1day = (1,1);
        }
        
    } else {
            # assemble $PARAM{'extra_range'} of uids to uid string
        @query_list_1day = splice(@query_big, 0, $PARAM{'extra_range'});
        $dispmax = $PARAM{'extra_range'};
        $counter = $PARAM{'extra_range'} + 1;
    }

    unless (@query_list_1day) {
        next unless ($day == 0);
    }

    $uidstring=join(',', @query_list_1day);
            
    my $search_db = $search_type;
#   if ($search_db eq 'nucleotide') {
#       $search_db = 'sequence';
#   }
    $docstring = join '&', ("db=$search_db",
                "id=$uidstring",
                "tool=$PARAM{'tool'}",
                "email=$sender",
                "retmax=$dispmax",
                'retmode=xml'
                );
    
    print "$day days old:  $uidstring\n";

    if ($day == 0){
        $second_visit_result .= 
        &tab('TR')
        .&tab('TD')
        .'<BR>';

            $no_html = &set_flag;
        $second_visit_result_mail .= 
        &tab('TR')
        .&tab('TD')
        .${break};
            $no_html = 0;

        #zero-day-old records: retrieve them
        $hits{$alias} = $#query_big + $#query_list_1day + 2;
        unless ($#query_list_1day < 0){
        # get the results of the HTTP-request
        # requires: $PARAM{'retrieve_URL'}(global) $docstring   
        # produces: $uid_result
        $second_visit_result .= 
            &tab('TABLE', ' WIDTH="100%" CELLSPACING="0" CELLPADDING="0"');

                $no_html = &set_flag;
        $second_visit_result_mail .= 
            &tab('TABLE', ' WIDTH="100%" CELLSPACING="0" CELLPADDING="0"');
                $no_html = 0;

        print "retrieving full reports from NCBI\nURL:\n$PARAM{'retrieve_URL'}?$docstring\n";       
        $time_1 = time if ($PARAM{'log_queries'});

        if ($simulation_file) {
            $tmp_result = '';
            if (open (IN, "$simulation_file")) {
            while (<IN>) {
                $tmp_result .= $_;
            }
            close IN;
            }
        } else {
            ($tmp_result,$connection_error) = &make_HTTP($PARAM{'retrieve_URL'},$docstring);
        }

            # log query and traffic
        if ($PARAM{'log_queries'}) {
            if ($PARAM{'from_quick'}) {
            &log_query($time_1,4,length($docstring),length($tmp_result));
            } else {
            &log_query($time_1,2,length($docstring),length($tmp_result));
            }
        }

            # retry in case Server Error occured
        while ( ( $tmp_result =~ /Server Error/s
              or
              $connection_error
              or
              $tmp_result !~ /<esummaryresult>/si
              )
            and
            $retry_local < $PARAM{'retry'}
            ) {
            my $text = ($connection_error ?
                'Connection error' :
                'Server error');
            $retry_local++;
            my $time_now = localtime;
            print "\nWARNING at $time_now: $text, starting retry $retry_local...\n";
            warn "WARNING at $time_now $PARAM{'id'}: $text, retry $retry_local for query $uidstring at second visit\n";
            print "sleeping for $PARAM{'break'} seconds...\n";
            sleep($PARAM{'break'}); 
            $time_1 = time if ($PARAM{'log_queries'});
            ($tmp_result,$connection_error) = &make_HTTP($PARAM{'retrieve_URL'},$docstring);
                # log query and traffic
            if ($PARAM{'log_queries'}) {
            if ($PARAM{'from_quick'}) {
                &log_query($time_1,'4.'.$retry_local,length($docstring),length($tmp_result));
            } else {
                &log_query($time_1,'2.'.$retry_local,length($docstring),length($tmp_result));
            }
            }
        }

        # write into output file how much has been done
#       &update_output;

        my $counter2 = 0;

        unless ($tmp_result =~ /<\/eSummaryResult>/si) {
            print STDERR "$PARAM{'id'} No end tag for results from query $alias!\n";
            # put into log file
            print "No end tag for results from query $alias:\n$tmp_result\n\n";
            $tmp_result = "<BR><CENTER><BIG><B>Received only partial results for this query - check logfile for content</B></BIG></CENTER><BR>\n";
            if ($PARAM{'quickstart'}) {
            $tmp_result .= "\n<CENTER><BIG><B>PubCrawler advises:</B>\n<B>-\&gt  <A HREF=\"http://pubcrawler.gen.tcd.ie/quickstart.html\">restart your jobs manually!</A></B></BIG></CENTER><BR>\n" ;
            $net_failure = 1;
            $advise = 1;
            }
        } else {
            # parse XML results
            if ($tmp_result =~ /<eSummaryResult>(.*)<\/eSummaryResult>/si) {
            $tmp_result = $1;
            if ($tmp_result =~ /<ERROR>(.*)<\/ERROR>/i) {
                $second_visit_result .= "ERROR: $1\n";
                $second_visit_result_mail .= "ERROR: $1\n";
            }
            while ($tmp_result =~ /<DocSum>/si) {
                $tmp_result =~ s/<DocSum>(.*?)<\/DocSum>//si;
                my $tmp_record = $1;
                my $checkbox = $checkbox_name{$search_type};
                $second_visit_result .= &format_record($search_type,$PARAM{'results_format'},$tmp_record,++$counter2,$checkbox,'website','website');

                unless ($mail_features{'javascript'}) {
                $checkbox = '';
                }
                my $mail_record = &format_record($search_type,$PARAM{'mail_results_format'},$tmp_record,$counter2,$checkbox,$mail_features{'html'},$mail_features{'entrez_links'});
                if ($mail_features{'text'}) {
                $second_visit_result_mail .= $mail_record;
                } else {
                $full_records_mail .= $mail_record;
                }
            }
            } else {
            warn;
            }
        }
            
        if ($counter2 > 1) {
            $second_visit_result .= 
            &tab('TR')
            .&tab('TD', ' COLSPAN=3')
            ."\n<A HREF=\"$summary_link\&db=$retrieve_db{$search_type}\&dispmax=$counter2\&list_uids=$uidstring\">Retrieve this group of hits</A><BR>"
            .&tab('/TD')
            .&tab('/TR');
            
            # mail file:
            my $insert_text = '';
            if ($mail_features{'entrez_links'}) {
            $insert_text = "\n<A HREF=\"$summary_link\&db=$retrieve_db{$search_type}\&dispmax=$counter2\&list_uids=$uidstring\">Retrieve this group of hits</A><BR>";
            unless ($mail_features{'html'}) {
                $insert_text = "\nRetrieval of above hits: $summary_link\&db=$retrieve_db{$search_type}\&dispmax=$counter2\&list_uids=$uidstring";;
            }
            }

                    $no_html = &set_flag;
            $second_visit_result_mail .= 
            &tab('TR')
            .&tab('TD', ' COLSPAN=3')
            .$insert_text
            .&tab('/TD')
            .&tab('/TR');
                    $no_html = 0;
        }

        $second_visit_result .= 
            &tab('/TABLE');

                $no_html = &set_flag;
        $second_visit_result_mail .= 
            &tab('/TABLE');
                $no_html = 0;

        } else { # unless ($#query_list_1day < 0){
        $second_visit_result .= "<b>No new records for \'$alias\' today</b><BR>";
        $second_visit_result_mail .= "${boldIn}No new records for \'$alias\' today${boldOut}${break}";      
        }

    } else { # if ($day == 0){

        $second_visit_result .=
        &tab('TR')
        .&tab('TD');

            $no_html = &set_flag;
        $second_visit_result_mail .=
        &tab('TR')
        .&tab('TD');
            $no_html = 0;

        #older records: make a hypertext link to them, if they exist
        unless ($#query_list_1day == -1){
        my $retrieve_number = @query_list_1day;
        $number_of_1day_records = $#query_list_1day + 1;
        $button = "\nMORE: $day-day-old records for \'$alias\'  <A HREF=\"$summary_link\&db=$retrieve_db{$search_type}\&dispmax=$retrieve_number\&list_uids=$uidstring\">("
            .($#query_big >= 0?"1 - ":"")
            ."$number_of_1day_records)</A> ";

#       $button =~ s/ /\&nbsp;/g;
#       $button =~ s/<A\&nbsp;HREF/<A HREF/g;

        $second_visit_result .= $button;
        
        if ($mail_features{'entrez_links'}) {
            if ($mail_features{'html'}) {
            # keep button as it is
            } else {
            $button = "\nMORE: $day-day-old records for \'$alias\'("
                .($#query_big >= 0?"1 - ":"")
                ."$number_of_1day_records),  $summary_link\&db=$retrieve_db{$search_type}\&dispmax=$retrieve_number\&list_uids=$uidstring";
            } 
        } else {
            $button = "\nMORE: $day-day-old records for \'$alias\'("
                .($#query_big >= 0 ? "1 - " : '')
                ."$number_of_1day_records)";
        }
        $second_visit_result_mail .= $button;
        }
    }
    # create buttons for excessive entries
    while (@query_big) {
        @query_tmp = splice(@query_big, 0, $PARAM{'extra_range'});
        my $retrieve_number = @query_tmp;
        $uidstring=join(',', @query_tmp);
        $docstring = join '&', ('cmd=Retrieve',
                    "db=$search_type",
                    "list_uids=$uidstring",
                    "dopt=$dopt",
                    "tool=$PARAM{'tool'}",
                    "dispmax=$PARAM{'extra_range'}"
                    );
        $number_of_1day_records = $counter + $#query_tmp;
            # create a link for excessive reports...
        my $more_text;
        my $more_text_mail;
        my $button_mail;
        if ($button) {
            $button = "  <A HREF=\"$summary_link\&db=$retrieve_db{$search_type}\&dispmax=$retrieve_number\&list_uids=$uidstring\">($counter - $number_of_1day_records)</A>";

        if ($mail_features{'entrez_links'}) {
            if ($mail_features{'html'}) {
            $button_mail = $button;
            } else {
            $button_mail = " ($counter - $number_of_1day_records): $summary_link\&db=$retrieve_db{$search_type}\&dispmax=$retrieve_number\&list_uids=$uidstring\n";
            }
        } else {
            $button_mail = " ($counter - $number_of_1day_records)";
        }
        } else {
        if ($day > 0) {
            $more_text = "\nMORE: $day-day-old records";
            $more_text_mail = "\nMORE: $day-day-old records";
        } else {
            $more_text = "\n<B>MORE: $day-day-old records</B>";
            $more_text_mail = "\n${boldIn}MORE: $day-day-old records${boldOut}";
        }
            $button = "$more_text for \'$alias\'  <A HREF=\"$summary_link\&db=$retrieve_db{$search_type}\&dispmax=$retrieve_number\&list_uids=$uidstring\">($counter - $number_of_1day_records)</A>";

        if ($mail_features{'entrez_links'}) {
            if ($mail_features{'html'}) {
            $button_mail = $button;     
            } else {    
            $button_mail = "$more_text_mail for \'$alias\'  ($counter - $number_of_1day_records): $summary_link\&db=$retrieve_db{$search_type}\&dispmax=$retrieve_number\&list_uids=$uidstring\n";
            }
        } else {
            $button_mail = "$more_text_mail for \'$alias\'  ($counter - $number_of_1day_records)";
        }
        }
#       $button =~ s/ /\&nbsp;/g;
#       $button =~ s/<A\&nbsp;HREF/<A HREF/g;
        $second_visit_result .= $button;

        # mail file:
        $second_visit_result_mail .= $button_mail;
        
        $counter = $number_of_1day_records + 1;
    }   
    $second_visit_result .=
        &tab('/TD')
        .&tab('/TR');

        $no_html = &set_flag;
    $second_visit_result_mail .=
        &tab('/TD')
        .&tab('/TR');
        $no_html = 0;
    
    }
#    $second_visit_result .= 
#   &tab('/TD')
#   .&tab('/TR');

    unless ($mail_features{'text'}) {
    $second_visit_result_mail = $full_records_mail;
    }

    return ($second_visit_result,$second_visit_result_mail);
}#end sub second_visit


################################################################################
# SORT_BY_TIME_FIRST_SEEN_AND_UID
# subroutine to sort UIDs into increasing age order in output:
# sort by (1) recent time_first_seen and then (2) high UID number
#
# COMMENT: this could probably be simplified by just sorting the UIDs numerically
#          because that seems to relate to the age of the item
#          but just in case I'll leave it as it is 
#          also, the neighbourhood searches should be returned in significance order
#          and not by date (i.e., numerical sorting)  (Karsten Hokamp, Feb '04)
sub by_time_first_seen_and_uid {
    if ( ${ $db{$alias_global} }{$a} > ${ $db{$alias_global} }{$b} ) {
        return -1;
    }elsif ( ${ $db{$alias_global} }{$a} < ${ $db{$alias_global} }{$b} ) {
        return 1;
    }elsif ( ${ $db{$alias_global} }{$a} == ${ $db{$alias_global} }{$b} ) {
    if ($a < $b) {
        return 1;
    }elsif ($a==$b) {
        0;
    }elsif ($a > $b) {
        return -1;
    }
    }
}#return from sub by_time_first_seen_and_uid
    

################################################################################
# TRAILER
# subroutine to make the trailer for the HTML file
# with a copy of the config file (if include_config was YES in the config file)
# called by MAIN
# requires: $timestamp $PARAM{'include_config'} $config_file $prog_name 
# returns: $trailer
sub trailer {
    my $mail_version = shift;
    my $trailer = '';

    if ($mail_version) {
    $no_html = &set_flag;
    }


# NCBI query box                                                                                                                             
my $query_box = "
<CENTER><A HREF=\"#disclaimer\">[next]</A>&nbsp;<A HREF=\"#TOP\">[top]</A></CENTER><BR>
For your convenience, the following form allows to carry out searches at the US National Library of Medicine (NLM) and PubMed through <A HREF=\"http://www.ncbi.nlm.nih.gov/\">NCBI</A>:
<table>
<tr>
<td class=\"SMALL1\" nowrap>
&nbsp;Search<small>
<select name=\"db\" onChange=\"DbChange(this);\">
<option selected value=\"PubMed\">PubMed</option>
<option value=\"protein\">Protein</option>
<option value=\"nucleotide\">Nucleotide</option>
<option value=\"structure\">Structure</option>
<option value=\"genome\">Genome</option>
<option value=\"books\">Books</option>
<option value=\"domains\">3D Domains</option>
<option value=\"cdd\">Domains</option>
<option value=\"gene\">Gene</option>
<option value=\"geo\">GEO</option>
<option value=\"gds\">GEO DataSets</option>
<option value=\"journals\">Journals</option>
<option value=\"mesh\">MeSH</option>
<option value=\"ncbisearch\">NCBI Web Site</option>
<option value=\"omim\">OMIM</option>
<option value=\"pmc\">PMC</option>
<option value=\"popset\">PopSet</option>
<option value=\"snp\">SNP</option>
<option value=\"taxonomy\">Taxonomy</option>
<option value=\"unigene\">UniGene</option>
<option value=\"UniSts\">UniSTS</option>
</select>
<input name=\"orig_db\" type=\"hidden\" value=\"PubMed\"></small>
for
<input name=\"term\" size=\"45\" type=\"TEXT\" value=\"<insert search and click on 'Go' (do not use return key)>\">                     
<input type=\"button\" value=\"Go\" onClick=\"ncbi_query(this.form);\">
<input name=\"Clear\" type=\"button\" value=\"Clear\" onClick=\"this.form.term.value='';this.form.term.focus();\">
</td>
</tr>
</table>
<BR>
";

my $form_end = "
</FORM>
<form method=\"post\" action=\"\" name=\"gotoNCBI\"></form>
";

my $disclaimer = "
<BR>
<CENTER>
<TABLE WIDTH=\"95%\" BGCOLOR=LIGHTGREY>
<TR>
<TD><CENTER>
PubCrawler was developed and is hosted by <A HREF=\"http://wolfe.gen.tcd.ie\">Ken Wolfe\'s lab</A> in the Genetics Department, Trinity College Dublin, Ireland. 
<BR>It has no affiliation with NCBI.
<BR>This service is free and comes with ABSOLUTELY NO WARRANTY. 
<BR>It is provided to the public in the hope that it is useful.
<BR>All records stem from the National Library of Medicine and were retrieved by PubCrawler through NCBI\'s <A HREF=\"http://eutils.ncbi.nlm.nih.gov/entrez/query/static/eutils_help.html\">E-utils tools</A>.
<BR>Please take a look at <A HREF=\"http://eutils.ncbi.nlm.nih.gov/About/disclaimer.html\">NCBI\'s Disclaimer</A> for more information about their disclaimers and copyrights.
</CENTER>
</TD>
</TR>
</TABLE>
</CENTER>
<BR>
";

 
    if ($mail_version) {
    unless ($mail_features{'html'}) {
        $disclaimer = "

PubCrawler was developed and is hosted by Ken Wolfe\'s lab in the Genetics Department, Trinity College Dublin, Ireland. 
It has no affiliation with NCBI.
This service is free and comes with ABSOLUTELY NO WARRANTY. 
It is provided to the public in the hope that it is useful.
All records stem from the National Library of Medicine and were retrieved by PubCrawler through NCBI\'s E-utils tools.
Please take a look at NCBI\'s Disclaimer for more information about their disclaimers and copyrights.

";
    }
    }
    
    my $generated = "<small>generated by <a href=\"$home_link\">PubCrawler version $version_number</a> on $timestamp</small>";
    my $back_to_top = "<CENTER><A HREF=\"#TOP\">[back to top]</A></CENTER><BR>\n";
    my $disclaimer_header = "<H3><font color=\"#ffffff\">::::::</font>&nbsp;<A NAME=\"disclaimer\">Disclaimer and Copyright</A></H3>";
    my $query_box_header = "<H3><font color=\"#ffffff\">::::::</font>&nbsp;<A NAME=\"query_box\">New Query</A></H3>";
    my $retrieval_header = "<H3><font color=\"#ffffff\">::::::</font>&nbsp;<A NAME=\"retrieval\">Retrieval of selected items</A></H3>";
    
    my $ncbi_buttons_out = $ncbi_buttons;

    if ($mail_version) {
    
    $ncbi_buttons_out = "<CENTER><A HREF=\"#query_box\">[next]</A>&nbsp;<A HREF=\"#TOP\">[top]</A></CENTER>\n$ncbi_buttons\n" if ($mail_features{'text'});
    unless ($mail_features{'html'}) {
        $ncbi_buttons_out = '';
        $query_box = '';
        $generated = "generated by PubCrawler version $version_number on $timestamp";
        $back_to_top = '';
        $disclaimer_header = "\n\n:::::: Disclaimer and Copyright";
        $query_box_header = '';
        $retrieval_header = '';
    }
    
    unless ($mail_features{'javascript'}) {
        $ncbi_buttons_out = '';
        $query_box = '';
        $form_end = '';
        $query_box_header = '';
        $retrieval_header = '';
    }
    } else {
    $ncbi_buttons_out = "<CENTER><A HREF=\"#query_box\">[next]</A>&nbsp;<A HREF=\"#TOP\">[top]</A></CENTER>\n$ncbi_buttons\n";
    }

    unless ($mail_version) {
    $trailer = "\n<!-- Trailer -->";
    }

    $trailer = 
    &tab('TR'," bgcolor=\"$PARAM{'bg'}\"")
    .&tab('TD')
    .$retrieval_header
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR')
    .&tab('TD')
    .$ncbi_buttons_out
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR'," bgcolor=\"$PARAM{'bg'}\"")
    .&tab('TD')
    .$query_box_header
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR')
    .&tab('TD')
    .$query_box
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR'," bgcolor=\"$PARAM{'bg'}\"")
    .&tab('TD')
    .$disclaimer_header
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR')
    .&tab('TD')
    .$disclaimer
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR')
    .&tab('TD')
    .$back_to_top
    .&tab('/TD')
    .&tab('/TR')
    .&tab('TR')
    .&tab('TD')
    .$generated;

    my $ref = basename $PARAM{'html_file'};
    
    if ($PARAM{'system'} eq 'unix') {
    $hostname = `hostname` || 'no hostname defined';
    chomp $hostname;
    $hostname = "\n<!-- host: $hostname -->\n";
    }
    
    if ($backup_file) {
    my $text = "<small>, <B>(access to <a href=\"$backup_file\">previous results</a>)</B></small>$hostname\n";
    if ($mail_version) {
        unless ($mail_features{'html'}) {
        $text = ", previous results at $backup_file";
        }
    }
    # if the pubcrawler links were chosen
    # we don't need it here because it exists 
    # in the left bar already
    if ($mail_features{'pubcrawler_links'}) {
        $text = '';
    }
    $trailer .= " $text\n";
    
    }

    if ($PARAM{'include_config'} =~ /^y|1/i ) {
        # append configuration file:
    my $text = "<HR>PubCrawler configuration file (<B>".(basename $config_file)."</B>) reads:<BR>\n";
    if ($mail_version) {
        unless ($mail_features{'html'}) {
        $text = "\nPubCrawler configuration file (".(basename $config_file).") reads:\n";
        }
    }

    my $append = 
        &tab('TABLE',' bgcolor="#efefef" WIDTH="95%"')
        .&tab('TR')
        .&tab('TD')
        .$text;

    open (CONFIG,"$config_file") ||
        die "$prog_name ERROR $PARAM{'id'}: cannot open configuration file $config_file";
    while (<CONFIG>){
        next if (/^</);
        if ($mail_version) {
        $append .= "$_${break}";
        } else {
        $append .= "$_<br>";
        }
    }
    close (CONFIG);
    $append .= &tab('/TD')
        .&tab('/TR')
        .&tab('/TABLE'); # this closes off the table with config info


    $trailer .= $append;

    }

    my $separator = "\n<!-- End of trailer -->\n";
    my $div = "\n</DIV>";

    if ($mail_version) {
    unless ($mail_features{'html'}) {
        $separator = '';
        $div = '';
    }
    }
    $trailer .= 
    &tab('/TD')
    .&tab('/TR')
    .$separator
    .&tab('/TABLE')
    .$div
    .$form_end
    .&tab('/TD')
    .&tab('/TR')
    .&tab('/TABLE');

    my $end_tags = "\n</BODY>\n</HTML>\n";
    if ($mail_version) {
    unless ($mail_features{'html'}) {
        $end_tags = '';
    }
    }

    $trailer .= $end_tags;

    $no_html = 0;

    return ($trailer);
}#return from sub trailer


sub set_flag {
    if ($mail_features{'html'}) {
    return 0;
    } else {
    return 1;
    }
}

sub tab {

    # do a proper indent of table tabs, so that HTML source code
    # is easier to debug:
    my $content = shift;
    my $addition = shift || '';

    if ($no_html) {
    return '';
    }

    if ($indent < 0) {
    $indent = 0;
    }

    if ($content =~ /^\//) {
    if ($prev_indent eq '+') {
        $indent--;      
    }
    } else {
    if ($prev_indent eq '-') {
        $indent++;
    }
    }

    my $out = "\n";
    $out .= ' ' x $indent;
    $out .= "<$content$addition>";

    if ($content =~ /^\//) {
    $indent--;
    $prev_indent = '-';
    } else {
    $indent++;
    $prev_indent = '+';
    }
    
    return $out;
}

################################################################################
# TIMESTAMP
#subroutine to make timestamp from localtime function
#called by MAIN
#requires: nothing   returns: $dateline $timestamp  
sub timestamp{
    my $mode = shift || '';
    my ($sec,$min,$hour,$mday,$mon,$year,$wday)=localtime(time);
    foreach ($hour, $min, $sec){
    if (length($_) == 1) {
        $_ = "0".$_;
    }
    }
          # match number of day to explicit name
    my $dayname = 
    ('Sunday',
     'Monday',
     'Tuesday',
     'Wednesday',
     'Thursday',
     'Friday',
     'Saturday')[$wday];
          # match number of month to explicit name
    my $monthname =
    ('Jan',
     'Feb',
     'Mar',
     'Apr',
     'May',
     'June',
     'July',
     'Aug',
     'Sept',
     'Oct',
     'Nov',
     'Dec')[$mon];
        # change year to four-digit style
    $year+=1900;
        # return dateline and timestamp
    if ($mode eq 'mail') {
    return "$mday $monthname";
    } elsif ($mode eq 'txt') {
    return "$dayname $mday $monthname $year";
    } else {
    return("<A NAME=\"TOP\">$dayname $mday $monthname $year</A>", 
           "$dayname $mday $monthname $year at $hour:$min:$sec $timezone");
    }
}#return from sub timestamp
    
    
################################################################################
# MAKE_HTTP
#subroutine to make HTTP connections
#called by &first_visit and &second_visit
#requires: $PARAM{'search_URL'}/$PARAM{'retrieve_URL'} $docstring   produces: $result
sub make_HTTP {

    # mostly copied from 'man LWP'
    my $request_URL = shift;
    my $request_string = shift;
    my $rescontent;
    my $connection_error = 0;

    if ($PARAM{'lynx'}) {
    # make internet-connection through 
    # alternative command-line browser:
    unless ($PARAM{'system'} =~ /unix/i) {
        die "$prog_name ERROR: system call for \'Lynx\' only works for unix";
    } else {
        $request_string = $request_URL.'?'.$request_string;
        $rescontent = `$PARAM{'lynx'} -source \'$request_string\'`;
        return $rescontent;
    }   
    }
        # initialize user agent
    my $ua = new LWP::UserAgent;
    my $req; #new request object
        # set time out
    $ua->timeout($PARAM{'time_out'});
        # set proxy
    if ($proxy_string) {
    $ua->proxy(http  => "$proxy_string");
    }
        # initialize request
    if ($request_URL eq $PARAM{'search_URL'}) {
#   or 
#   $request_URL eq $PARAM{'neighbour_URL'}) {
    # unfortunately POST doesn't seem to work for searching
    $req = new HTTP::Request GET => $request_URL.'?'.$request_string;
    } else {
    # use POST method for retrieving reports
    $req = new HTTP::Request POST => $request_URL;
    $req->content_type('application/x-www-form-urlencoded');
    $req->content($request_string);
    }

    $req->proxy_authorization_basic("$PARAM{'proxy_auth'}", "$PARAM{'proxy_pass'}") 
    if ($PARAM{'proxy_pass'} and $PARAM{'proxy_auth'});

        # get result of request
    my $res = $ua->request($req);
    if ($res->is_success) {
    #print "\n res content \n";
    #print $res->content;
    } else {
    $connection_error = 1;
    print "No reply to HTTP-request - bad luck this time\n";
    }

    $rescontent = $res->content;

    #macchange CRLF
    $rescontent =~ s/[\015\012]/\015/g if ($PARAM{'system'} =~ /macos/i);

    # holdings change
    if ($PARAM{'tool'} =~ /holding=/i) {
    $rescontent =~ s/($retrieve_URL_def\?)/$1tool=$PARAM{'tool'}\&/sig;
    }
    return($rescontent,$connection_error);
}#end of sub make_HTTP
    
#############################################################
    
sub connection_test{
    # tests if test_URL can be reached through proxy...
    if ($PARAM{'no_test'}) {
    print "suppressed!\n";
    return 0;
    }
    my $proxy_tmp = shift;
    my $url = shift;
    my $ua = new LWP::UserAgent;
    
    $ua->timeout($PARAM{'time_out'});
    $ua->proxy(http  => $proxy_tmp) unless ($proxy_tmp eq 'no_proxy');
    
    my $req = new HTTP::Request 'GET',"$url";
    
    $req->proxy_authorization_basic("$PARAM{'proxy_auth'}", "$PARAM{'proxy_pass'}") 
    if ($PARAM{'proxy_pass'} and $PARAM{'proxy_auth'} and ($proxy_tmp ne 'no_proxy'));
    my $res = $ua->request($req);
    if ($res->is_success) {
    return 1;
    } else {
    return 0;
    }
}

sub update_output {
        # writes new percentage into output file

    $update_counter += $update_unit; 
    if ($update_counter > 101) {
    warn "$update_counter done? No update for output file...\n";
    return;
    }
    $update_message = "<B>$update_counter\%</B> done";
    if (length($update_message) < $update_message_len) {
    my $diff = $update_message_len - length($update_message);
    for (my $i = 0; $i < $diff; $i++) {
        $update_message .= ' ';
    }
    }
        # write to output file
    sysseek(OUT,"-$update_message_len",2);
        # set new length for next sseek
    $update_message_len = length($update_message);
    &sys_print('OUT',"$update_message");
}
    
sub proxy_setting{    
    # configure proxy and check if internet access provided...
        # writes into global $proxy_string

    return if ($proxy_string); #configured already

    my $proxy_tmp = shift;
    my $check_mode = shift;
    my @proxy_tried = ();  #list of unsuccessful proxy configs
    my $proxy_config = ''; #address for proxy auto configuration
    my @proxy_config = (); #return from pac-address
    my $proxy_conf;

    return if ($PARAM{'lynx'});

    unless ($PARAM{'no_test'}) {
    if ($check_mode) {
        print STDERR "\n\t - checking internet access through proxy...";
    } else {
        print STDERR "\nTesting internet access through proxy...\n" unless ($PARAM{'mute'});
    }
    }
    # if the address of a proxy-configuration file is given
    # retrieve content to configure proxy settings...
    # (this is detected by a slash somewhere BEFORE the end of the string)
    if ($proxy_tmp =~ /\..*\/\w+/) {
    @proxy_tried = ();
    $proxy_conf = ($proxy_tmp =~ /^http:\/\//)?'':'http://' ;
    $proxy_conf .= $proxy_tmp;
    if ($PARAM{'proxy_port'}) {
        $proxy_conf .= ($PARAM{'proxy_port'} =~ /:(\d+)/)?$PARAM{'proxy_port'}:":$1";
    }
            # retrieve configuration data
    @proxy_config = split /\n/, get("$proxy_conf"); 

    # for more info on proxy auto configuration (Netscape) look up
    # http://developer.netscape.com/docs/manuals/proxy/adminux/autoconf.htm

    foreach (@proxy_config) {
        my @tmp;
        # extract proxy autoconfig information
        next unless (/PROXY/);
        (undef,@tmp) = split /PROXY/;
            # if keyword 'PROXY' found...
        foreach (@tmp) {
        my $server_test;
        my $port_test;
            # ... check the next word for a pattern
            # that looks like server (+port) specification
        if (/((\w+\.)+\w+)(:\d+)?/) {
            $server_test = $1;
            $port_test = $3;
            # test connection to proxy...
            $proxy_string = 'http://'.$server_test.$port_test.'/';
            return if ($PARAM{'no_test'});          
            if (&connection_test($proxy_string,$PARAM{'test_URL'})
            or $PARAM{'test_URL'} eq '') {
            last; # found a working proxy -> exiting test
                  # (or can't test it)
            } else {
            push @proxy_tried, $proxy_string;
            $proxy_string = '';
            }
        }
        last if ($proxy_string);
        }
        last if ($proxy_string);
    }
    return if ($PARAM{'no_test'});
    if ($proxy_string) {
        if ($check_mode) {
        unless ($PARAM{'test_URL'}) {
            print STDERR "no test-URL available!\n";
        } else {
            print STDERR "alright\n";
        }
        } elsif (! $PARAM{'mute'}) {
        unless ($PARAM{'test_URL'}) {
            print STDERR "No test-URL available!\n";
            print STDERR "Proxy setting: $proxy_string\n";
        } else {
            print STDERR "Successfully received the test URL!\n";
            print STDERR "Internet access through proxy ($proxy_string) seems o.k.\n";
        }
            print STDERR "Continuing with program...\n";
        }
    } else {
        if ($check_mode) {
        $error++;
        print STDERR "\n\t   ERROR $error: Can\'t configure proxy!\n";
        push @error, "Problems encountered when trying to access the test-URL.\n\tPlease check the test-URL:\n\t\'$PARAM{'test_URL'}\'\n\tand your proxy settings (command line or ".(&line_number($config_file,'^proxy\b')).')'.($proxy_tmp?":\n\t\'$proxy_tmp\'".($PARAM{'proxy_port'}?", \'$PARAM{'proxy_port'}\'":"")." evaluated to \'$proxy_string\'":"");
        } else {
        print STDERR "\n\nERROR $PARAM{'id'}:\n";
        print STDERR "Could not configure proxy from \'$proxy_tmp\'\n";
        foreach (@proxy_tried) {
            print STDERR "Tried proxy $_ without success\n";
        }
        print STDERR "Please check your proxy configuration or the test URL\n($PARAM{'test_URL'})\n";
        exit $EXIT_FAILURE;
        }
    }
    } else {
    # test if given proxy server (and port) is working...
    $proxy_string = 'http://' unless ($proxy_tmp =~ /^http:\/\//);
    $proxy_string .= $proxy_tmp;
    if ($PARAM{'proxy_port'}) {
        $proxy_string .= ($PARAM{'proxy_port'} =~ /:(\d+)/)?$PARAM{'proxy_port'}:":$PARAM{'proxy_port'}";
    }
    $proxy_string .= '/' unless ($proxy_string =~ /\/$/);
    return if ($PARAM{'no_test'});
    if ($PARAM{'test_URL'} eq '') {
        if ($check_mode) {
        print STDERR "no test-URL available!\n";
        } elsif (! $PARAM{'mute'}) {
        print STDERR "No test-URL available!\n";
        print STDERR "Proxy setting: $proxy_string\n";
        print STDERR "Continuing with program...\n";
        }
    } elsif (&connection_test($proxy_string,$PARAM{'test_URL'})) {
        if ($check_mode) {
        print STDERR "alright\n";
        } elsif (! $PARAM{'mute'}) {
        print STDERR "Successfully received the test URL!\n";
        print STDERR "Internet access through proxy ($proxy_string) seems o.k.\n";
        print STDERR "Continuing with program...\n";
        }
    } else {
        if ($check_mode) {
        $error++;
        print STDERR "\n\t   ERROR $error $PARAM{'id'}: Can\'t reach test-URL!\n";
        push @error, "Problems encountered when trying to access the test-URL.\n\tPlease check the test-URL:\n\t\'$PARAM{'test_URL'}\'\n\tand your proxy settings (command line or ".(&line_number($config_file,'^proxy\b')).")".($proxy_tmp?":\n\t\'$proxy_tmp\'".($PARAM{'proxy_port'}?", \'$PARAM{'proxy_port'}\'":"")." evaluated to \'$proxy_string\'":"");
        } else {
        print STDERR "\n\nERROR $PARAM{'id'}:\n";
        print STDERR "Problems with proxy ($proxy_string) encountered:\n";
        print STDERR "Please check your proxy entries or the test URL\n($PARAM{'test_URL'})\n";
        exit $EXIT_FAILURE;
        }
    }
    }
}

################################################################################
#  CHECK_SETTING
sub check_setting {
    print "
$prog_name will check the setting of variables and the configuration 
of additional files that are important for the execution of the program.
Any errors or warnings encountered are marked as such.
At the end recommondations are given to solve any problems.

Please press <return> to continue...
";

    <>;

    my $db_file;
    my $orig_dir;
    my $field;
    my $pause = 1;
    my $check_header;
    my $dir_ok = '';
    my $error_now;
    my $proxy_err;
    my $val;
    my $mk_dir_rec = "The working directory you specified ('$PARAM{'work_dir'}') does not exist.\n\tPlease create the directory\n\t(on Unix: mkdir $PARAM{'work_dir'})\n\tor specify a different directory either through the command line option \'-d\'\n\te.g. $program -d <dir>\n\tor ".(&line_number($config_file,'^\s*#*\s*work_dir\b'))."\n\te.g. work_dir /home/user/$prog_name";
    my $write_dir_rec = "Your working directory '$PARAM{'work_dir'}' is not writeable.\n\tPlease change the permissions\n\t(on Unix: chmod +w $PARAM{'work_dir'})\n\tor specify a different directory either through the command line option \'-d\'\n\te.g. $program -d <dir>\n\tor ".(&line_number($config_file,'^\s*#*\s*work_dir\b'))."\n\te.g. work_dir /home/user/$prog_name";
    my $read_dir_rec = "Your working directory '$PARAM{'work_dir'}' is not readable.\n\tPlease change the permissions\n\t(on Unix: chmod +r $PARAM{'work_dir'})\n\tor specify a different directory either through the command line option \'-d\'\n\te.g. $program -d <dir>\n\tor ".(&line_number($config_file,'^\s*#*\s*work_dir\b'))."\n\te.g. work_dir /home/user/$prog_name";
    my $system_rec = "A bad value for your \$PARAM{'system'}-variable has been detected.\n\tPlease use the command line option '-os' to specify one of 'macos','win','unix'\n\t(whatever comes closest to your operating system)\n\te.g. $program -os \'mac\'\n\tor set the value manually ".(&line_number($config_file,'^\s*#*\s*system\b'))."\n\te.g. system mac";
    my $header_rec = "The header-file you specified (\'$PARAM{'header'}\') could not be found or read.\n\tPlease make sure that the path is specified correctly\n\t- either via command line option\n\t  e.g. $program -head \'/home/user/header.file\'\n\t- or ".(&line_number($config_file,'^\s*#*\s*header\b'))."\n\t  e.g. header /home/user/header.file\n\tand that the file is readable\n\t(under Unix: chmod +r $PARAM{'header'})";
    my $read_config_rec = "Your configuration file '$config_file' is not readable.\n\tPlease change the permissions\n\t(on Unix: chmod +r $config_file)\n\tor specify a different file either through the command line option \'-c\'\n\te.g. $program -c <config_file>\n\tor ".(&line_number($config_file,'^\s*#*\s*config_file\b'))."\n\te.g. config_file /home/user/$prog_name.config";
    print "Start checking...\n";
    
    # OPERATING SYSTEM:
    if ($PARAM{'system'} =~ /win|macos|$unix_flav/i) {
    print " - operating system defined as \'$orig_system\', alright\n";
    } else {
    $error++;
    print "   ERROR $error: bad value for variable \$PARAM{'system'}: \'$orig_system\'!\n";
    push (@error, $system_rec);
    }

    # WORKING DIRECTORY:
    unless ($PARAM{'work_dir'}) {
    print " - no working directory set, using the current directory, alright\n";
    $PARAM{'work_dir'}=$cwd;
    }
    print " - checking your working directory: \'$PARAM{'work_dir'}\'...\n";
    sleep($pause);
    if (! -e $PARAM{'work_dir'}) {
    $error++;
    print "\t   ERROR $error: Working directory \'$PARAM{'work_dir'}\' does not exist!\n";
    push (@error, $mk_dir_rec); 
    } elsif (! -w $PARAM{'work_dir'}) {
    $error++;
    print "\t   ERROR $error: Can not write to working directory!\n";
    push (@error, $write_dir_rec);      
    } elsif (! -r $PARAM{'work_dir'}) {
    $error++;
    print "\t   ERROR $error: Can not read from working directory!\n";
    push (@error, $read_dir_rec);               
    } else {
    print "\t - \'$PARAM{'work_dir'}\' is fully accessible, alright\n";
    $dir_ok = '1';
    }

    # CONFIGURATION FILE:
    print " - checking your configuration file \'$config_file\'...\n";
    sleep($pause);
    if ($cmd_line_cfg_file and $config_read) {
    print "\t - configuration file is readable, alright\n";
    } else {
        # first look in the working directory
    $orig_dir = $cwd;
    chdir($PARAM{'work_dir'});
    if (-e "$config_file") {
        if (-r "$config_file") {
        print "\t - configuration file accessible from your working directory, alright\n";
        } else {
        @_ = &empty_vars(@expect_val);
        if (@_ > 0) {
            $error++;
            print "\t   ERROR $error: Can not read your configuration file \'$config_file\' in $PARAM{'work_dir'}!\n";
            push (@error, $read_config_rec);    
        } else {
            print "\t   WARNING: mandatory variables are set but no configuration file could be read in $PARAM{'work_dir'}!\n";
            $warning++;
        }   
        }               
    } else {
        chdir($orig_dir);
        if (-e "$config_file") {
        if (-r "$config_file") {
            print "\t - configuration file accessible from your current working directory, alright\n";
        } else {
            if (@_ = &empty_vars(@expect_val)) {
            $error++;
            print "\t   ERROR $error: Can not read your configuration file \'$config_file\' in $orig_dir!\n";
            push (@error, $read_config_rec);    
            } else {
            print "\t   WARNING: mandatory variables are set but no configuration file could be read in $orig_dir!\n";
            $warning++;
            }           
        }
        } else {
        @_ = &empty_vars(@expect_val);
        if (scalar(@_) > 0) {
            $error++;
            print "\t   ERROR $error: Can not find your configuration file \'$config_file\'!\n";
            push (@error, "No configuration file for $prog_name could be found.\n\tPlease make sure that a file called \'$PARAM{'prefix'}.config\'\n\tis located in your current directory ($cwd)\n\tor in your working directory (\'$PARAM{'work_dir'}\')\n\tor in your home directory (\'$ENV{HOME}\')\n\tor specify a file on the command line\n\te.g. \'$program -c <config.file>\'");      
        } else {
            print "\t   WARNING: mandatory variables are set but no configuration file could be found!\n";
            $warning++;
        }
        chdir($orig_dir);
        $orig_dir = '';
        }
    }
    }

    if ($config_read) {
    # check that all mandatory fields have values
    $error_now = $error;
    unless (defined $PARAM{'html_file'} and $PARAM{'html_file'} ne '') {
        $error++;
        print "\t   ERROR $error: no file name specified for output HTML!\n";
        push (@error, "Please specify a file name for output HTML\n\tthrough a statement like \'html_file ${prog_name}_result.html\' in your configuration file\n\tor through the comand line option '-out ${prog_name}_result.html'");
    } 

    unless (defined $PARAM{'relentrezdate'} and $PARAM{'relentrezdate'} ne '') {
        $error++;
        print "\t   ERROR $error: no maximum age for database entries specified !\n";
        push (@error, "Please specify a maximum age for database entries (in days)\n\tby including a line like \'relentrezdate 180\' in your configuration file\n\tor through the comand line option '-relentrezdate 180'.\n\tOther valid entries are '1 year','2 years','5 years','10 years','no limit'");
    }
    
    unless (defined $PARAM{'fullmax'} and $PARAM{'fullmax'} ne '') {
        $error++;
        print "\t   ERROR $error: no maximum retrieval number of full reports specified !\n";
        push (@error, "Please specify a maximum number for for retrieval of full reports\n\tby including a line like \'fullmax 20\' in your configuration file\n\tor through the comand line option '-fullmax 20'.");
    }       
    
    unless (defined $PARAM{'search_URL'} and $PARAM{'search_URL'} ne '') {
        $error++;
        print "\t   ERROR $error: no URL for query specified !\n";
        push (@error, "Please specify a URL for the queries to be carried out\n\tby including a line like 'search_URL $search_URL_def'\n\tin your configuration file\n\tor through the comand line option '-q $search_URL_def'.");
    }       
        unless (defined $PARAM{'retrieve_URL'} and $PARAM{'retrieve_URL'} ne '') {
        $error++;
        print "\t   ERROR $error: no URL for report retrieval specified !\n";
        push (@error, "Please specify a URL for the reports to be retrieved from\n\tby including a line like 'retrieve_URL $retrieve_URL_def'\n\tin your configuration file\n\tor through the comand line option '-r $retrieve_URL_def'.");
    }       
        unless (defined $PARAM{'getmax'} and $PARAM{'getmax'} ne '') {
        $error++;
        print "\t   ERROR $error: no maximum number of entries specified !\n";
        push (@error, "Please specify a maximum number for database entries (in days)\n\tby including a line like \'getmax 200\' in your configuration file\n\tor through the comand line option '-getmax 200'.");
    }       
    unless (defined $PARAM{'viewdays'} and $PARAM{'viewdays'} ne '') {
        $error++;
        print "\t   ERROR $error: no value for viewdays specified !\n";
        push (@error, "Please specify a value for number of days that an entry will be shown\n\tby including a line like \'viewdays 5\' in your configuration file\n\tor through the comand line option '-viewdays 5'.");
    }

    unless (defined $PARAM{'include_config'} and $PARAM{'include_config'} ne '') {
        $error++;
        print "\t   ERROR $error: handling of configuration file is not specified !\n";
        push (@error, "Please specify if your configuration file should be appended to your output file\n\tby including a line like \'include_config Y\' in your configuration file\n\tor through the comand line option '-i'.");
    }
    foreach (@warning) {
        print "\t   WARNING: $_";
    }
    if ($error == $error_now) {
        if (@warning) {
        print "\t - ambiguities found in configuration file\n";
        } else {
        print "\t - configuration file looks fine\n";
        }
    }
    }
    sleep($pause);

    # DATABASE FILES
    if ($dir_ok) {
    print " - checking database..." ;
    sleep($pause);
    }
    $db_file = ($PARAM{'database'} or "$PARAM{'prefix'}.db");
    if ($PARAM{'work_dir'} ne $cwd) {
    chdir $PARAM{'work_dir'};
    }
    if (-e "$db_file" and $dir_ok) {
    unless (-w "$db_file") {
        $error++;
        print "\n\t   ERROR $error: Can\'t write to database \'$db_file\'!\n";
        push (@error, "Please make your database writeable (chmod +w $db_file)\n");
    } else {
        print "alright\n";
    }
    } else {
    print "\n\t - no database file found (\'$db_file\')\n\t - WARNING: initialization might take up a lot of space!\n" if ($dir_ok);
    $warning++;
    }
    # reverse changes to cwd and config_file
    if ($orig_dir) {
    chdir($orig_dir);
    $orig_dir = '';
    $config_file = '';
    }       

    # HEADER:
    print " - checking header...\n";
    sleep($pause);
    if (-e $PARAM{'work_dir'}.$joiner.$PARAM{'header'} and $PARAM{'header'}) {
    $check_header = $PARAM{'work_dir'}.$joiner.$PARAM{'header'};
    } else {
    $check_header = $PARAM{'header'};
    }
    if ($check_header) {
    print "\t - trying to read file \'$PARAM{'header'}\'...";
    if (-r $check_header) {
        print "alright\n";
    } else {
        $error++;
        print "\n\t   ERROR $error: Can not read file \'$check_header\'!\n";
        push (@error, $header_rec);
    }
    } else {
    print "\t - using automatically generated header for output file, alright\n";
    }    
    
    # INTERNET CONNECTION
    print STDERR " - checking the internet connection...";
    unless ($PARAM{'test_URL'}) {
    print STDERR "\n\t - WARNING: no test URL available, cannot carry out test!\n";
    $warning++;
    } else {
    if ($PARAM{'lynx'}) {
        my $rescontent = `$PARAM{'lynx'} -source \'$PARAM{'test_URL'}\'`;
        if ($rescontent =~ /\s*^\w+\: Can\'t access startfile/ or $rescontent eq '') {
        $error++;
        print "\n\t - Error $error: Can\'t reach test-URL!\n";
        push @error, "Problems encountered when trying to access the test-URL.\n\tPlease check the test-URL:\n\t\'$PARAM{'test_URL'}\'\n\tor the configuration of \'$PARAM{'lynx'}\'";
        } else {
        print "alright\n";
        }
    } elsif ($PARAM{'proxy'}) {
        if ($PARAM{'no_test'}) {
        print STDERR "\n\t - disabeling \'no_test\'-setting for check...";
        $PARAM{'no_test'} = 0;
        }
        foreach my $proxy_tmp (split /,/, $PARAM{'proxy'}) {
        &proxy_setting($proxy_tmp,'check');
        }
#       &proxy_setting('check');
    } else {
        if (&connection_test('no_proxy',$PARAM{'test_URL'})) {
        print "alright\n";
        } else {
        $error++;
        print "\n\t - Error $error: Can\'t reach test-URL!\n";
                $proxy_err = "Problems encountered when trying to access the test-URL.\n\tPlease check the test-URL:\n\t\'$PARAM{'test_URL'}\'\n\t";
                if ($PARAM{'proxy'}) {
                    $proxy_err .= "and your proxy settings (command line or line ".(&line_number($config_file,\'^\s*#*\s*proxy\b'))." of this script)".($PARAM{'proxy'}?":\n\t\'$PARAM{'proxy'}\'".($PARAM{'proxy_port'}?", \'$PARAM{'proxy_port'}\'":"")." evaluated to \'$proxy_string\'":"");
                } else {
                    $proxy_err .= "and consider using a proxy-server (using command line option \'-p\' \n\tor setting value for \'proxy\' ".(&line_number($config_file,'^\s*#*\s*proxy\b')).".";
                }
        push @error, $proxy_err; 
        }
    }
    }

    
    # E-MAIL SERVICE
    if ($PARAM{'notify'} or $PARAM{'mail'}) {
    print STDERR " - checking e-mail service...";
    sleep($pause);
    &mail_service;
    if ($PARAM{'notify'}) {
        print STDERR "\n\t - notification has been sent to $PARAM{'notify'}!\n";
    } else {
        print STDERR "\n\t - results have been sent to $PARAM{'mail'}!\n";
    }
    }

    # GIVE RECOMMENDATIONS:
    if ($error > 0) {
    print "\n$error ";
    print ($error > 1?'errors have ':'error has ');
    print "been detected!\nSome suggestions will be made next on how to solve any problems.\n\nPlease press <return> to continue...";
    <>;
    my $tip = 1;
    foreach (@error) {
        print "\nTIP $tip: $_\n";
        unless ($tip >= $error) {
        print "\nPress <return> to see next tip...";
        <>;
        }
        $tip++;
    }
    print "\nPress <return> to finish...";
    <>;
    print "\nPlease run \'$program -check\' again after any changes made!\n\nEnd of check!\n\n";
    } else {
    print "\nEnd of check, no error detected.";
    if ($warning > 0) {
        print " $warning warning".($warning > 1?"s.":".");
    }
    print "\nWith this setup the program should run without problems.\n\n";
    }
    print "*** For information on configuring PubCrawler visit\n";
    print "*** $link_gen/pubcrawler_download.html\n\n";

    # delete temporary file
    if (-e $tmp_file) {
    sleep 3;
    unlink $tmp_file;
    }

    exit($EXIT_SUCCESS);
}

################################################################################

sub line_number {
        # find a pattern in a file and print out 
        # the line-number where it occurs the first time
    my $file = shift;
    my $pattern = shift;
    my $line = 0;
    
    open (IN, "$file") or return 'in your configuration file';;
    while (<IN>) {
    $line++;
    if (/$pattern/) {
        return "at line $line of your configuration file";
    }
    }
    return 'in your configuration file';
}
    

################################################################################

sub read_db {
        # read in a database file which holds
        # previous uids and the time they were retrieved
        # ordered by aliases
    my $db_file = ($PARAM{'database'} or "$PARAM{'prefix'}.db");
    my $alias = '';
    my ($uid, $age);

    return unless (-e "$db_file");

    open (DB, "$db_file") 
    or die "$prog_name ERROR: cannot open database \'$db_file\' ";
#    $^W = 0; # switch off warning
    while (<DB>) {
    chomp;
      # identify alias by percent sign at beginning of line
    if (/^%(.*)/) {     
        $alias = $1;
    } elsif (/\d/) {
        ($uid,$age) = split;
        ${ $db{$alias} }{$uid} = $age;
        }
    }
#    $^W = $warn_stat; # set back warning status
    close DB;
}

################################################################################

sub save_db {
#save the updated database:
#(12 Oct 98: saving to a different filename (.temp) and then re-naming 
#to avoid losing the whole database if there's a crash)
    my $db_file = ($PARAM{'database'} or "$PARAM{'prefix'}.db");
    my $alias;
    my $uid;

    return if ($PARAM{'cmd_query'});
    open(LOG,">$db_file.temp") ||
     die "$prog_name ERROR: cannot write to database file $db_file.temp";
    
    foreach $alias (keys %aliases) {
    print LOG '%'."$alias\n";
    foreach $uid (keys %{ $db{$alias} }) {
        print LOG "$uid\t${ $db{$alias} }{$uid}\n";
    }
    }
    close(LOG);

    if ($PARAM{'system'} =~ /win/i) {
        # under Windows, the former database has to be
        # (re)moved before another file can take its name...
    move("$db_file", "$db_file.bak") or 
        warn "Can not move $db_file to $db_file.bak\n";
    }
    rename("$db_file.temp","$db_file") ||
    warn "$prog_name ERROR: cannot rename temp database file";
}

################################################################################

sub read_config {
        # read in configuration file to set values of variables
        # and to get search criterias, databases and according aliases
    my $field;
    my $val;
    my $alias;
    my $searchtype;
    my $query;
    my $line = 0;
    my %found = ();
    my $open_result;
    
    return if ($config_read);

    $open_result = open (CONFIG,"$config_file");
    unless ($open_result) {
    if ($PARAM{'check'}) {
        return;
    } else {
        die "$prog_name ERROR: cannot open configuration file $config_file";
    }
    }

    my @mail_features = ();

  WHILE:while (<CONFIG>){
    
        $line++;
    ($_) = split (/\#/);             # remove comments
    next unless ($_);
    ($_, undef) = split (/\</, $_, 2);             # remove HTML-tags
        s/\s*$//;                        # clean end of line from white-space
    next unless (/\w/);              # skip empty lines
    s/\s+/ /g;                       # reduce multiple whitespaces 
                                     # to single spaces
    unless (/^\s*$known_searchtypes\s+(.*)/) {      #load general setup data        
        ($field,$val)=split(/\s+/, $_, 2);
            # strip any leading or ending quotes
        $field =~ s/^'|"//;
                $field =~ s/"|'$//;

            # check if user is allowed to change
                # value of this variable
        unless (grep /\Q$field/, (@expect_val,@allowed_var)) {
        print STDERR "$prog_name WARNING $PARAM{'id'}: Invalid variable name: $field at line $line of config-file, skipping!\n" unless ($PARAM{'mute'});
        $warning++;
        next;
        }

            # skip if value has been set
            # by command line option already:
        next if ($PARAM{$field} or $PARAM{$field} eq '0');         
                                     
        if (defined $val) {
            # strip any leading or ending quotes
        $val =~ s/^'|"//;
                $val =~ s/"|'$//;
                    # convert leading tilde to HOME-directory
                $val =~ s/^~/$ENV{'HOME'}/ if ($ENV{'HOME'});
                    # set value
        if ($field eq 'mail_feature') {
            push @mail_features, $val;
        } else {
            $PARAM{$field} = $val;
        }
            }
    } else {                        
                # extract database, query and alias:
        $searchtype = $1; #($known_searchtypes are in brackets)
        $_ = $2;        #load string following search type
        if (/^\'/) {    #look for alias
        if (s/'/'/g > 2) {
            if ($PARAM{'check'}) {
            push @warning, "Too many aliases declared for $_\n";
            $warning++;
            } else {
            print STDERR "$prog_name WARNING $PARAM{'id'}: Too many aliases declared, dismissing $_!\n" unless ($PARAM{'mute'});
            }
        }
        (undef,$alias,$_) = split /\'/, $_, 3;
        } else {        # use query for alias if none specified
        $alias = $_;
        }
            # standard format for queries:
            my $query_orig = $_;  # store original query
        s/^\s*//;      # delete leading white space
                # avoid sending [all fields] delimiter
                # without them we avoid MeSH-checking
                # and get all hits
                # (worked for 'horizontal gene transfer')
            s/\[ *all( fields)? *\]//gi;
        s/\s+/\+/g;    #put in plusses
        $query = uc;                #convert to all uppercase

            # check if query exists already
        if ($query{$query}) {
        if (($query{$query}{'ALIAS'} eq $alias) and
            ($query{$query}{'DB'} eq $searchtype)) {
            if ($PARAM{'check'}) {
            push @warning, "Double entrance for $query\n"; 
            $warning++;
            } else {
            print STDERR "$prog_name WARNING $PARAM{'id'}: Double entrance for $query, dismissing one\n" unless ($PARAM{'mute'});
            }
        } else {
            $query .= '#2';
        }
        }
            # store query:
        push @query_order, $query;
        $query{$query}{'ALIAS'} = $alias;       
        $query{$query}{'DB'} = $searchtype;
            $query{$query}{'ORIG'} = $query_orig;

            # group queries according to their alias:
            my $item;
        foreach $item (@{ $aliases{$alias} }) {
           # make sure they all query the same database
        if ($query{$item}{'DB'} ne $searchtype) {
            if ($PARAM{'check'}) {
            push @warning, "Ambiguous entry for alias \'$alias\',\n\t      temporarily modifying one to $query.\n"; 
            $warning++;
            } else {
            print STDERR "$prog_name WARNING $PARAM{'id'}: Database of query $query differs from other queries with same alias, temporarily modifying alias.\n" unless ($PARAM{'mute'});
            }
            # query has #2 added, use this as a key instead of $alias
            push @{ $aliases{$query} }, $query;
            $query{$query}{'ALIAS'} = $query;
            next WHILE;
        }
        }
           # if we got here, all databases for queries sharing this alias
           # are the same and we can safely add this query...
            unless ($found{$alias}) {
            push @alias_order, $alias;
        $found{$alias} = '1';
        }
        push @{ $aliases{$alias} }, $query;
    }
    }
#    $^W = $warn_stat;      # set back warning status       
    close (CONFIG);
    
    # check for additional information
    # (this is mainly for the webservice):
    my $file = "$config_file.add";
    if (open (CONFIG, $file)) {
    $line = 0;
      WHILE:while (<CONFIG>){
      chomp;
      $line++;
      ($_) = split (/\#/);             # remove comments
      next unless ($_);
      ($_, undef) = split (/\</, $_, 2);             # remove HTML-tags
      s/\s*$//;                        # clean end of line from white-space
      next unless (/\w/);              # skip empty lines
      s/\s+/ /g;                       # reduce multiple whitespaces 
                                       # to single spaces

      my ($field,$val) = split / = /, $_, 2;
      next unless ($val =~ /\w/);

      # skip if value has been set
      # by command line option already:
      next if ($PARAM{$field} or $PARAM{$field} eq '0');         

      # strip any leading or ending quotes
      $val =~ s/^\'|\"//;
      $val =~ s/\"|\'$//;

      if ($field eq 'mail_feature') {
          push @mail_features, $val;
      } else {
          $PARAM{$field} = $val;
      }
      }
    close CONFIG;
    
    }

    if (@mail_features) {
    $PARAM{'mail_features'} = (join ',', @mail_features) unless ($PARAM{'mail_features'});
    }    
    
    if ($PARAM{'work_dir'}) {
    $PARAM{'work_dir'} .= $joiner unless ($PARAM{'work_dir'} =~ /$joiner$/);
    if ($PARAM{'system'} =~ /win/i) {
        # special treatment for windows paths
        # (due to joiner symbol \)
        $PARAM{'work_dir'} =~ s/\\\\/\\/g;
    }
    }

    $config_read = 1;
}

################################################################################

sub empty_vars {
      # checks if all submitted variables have values set
      # returns the name of variables without value
    my @expect_val = @_;
    my @no_val = ();

    foreach (@expect_val) {
    push (@no_val, $_) unless (length($PARAM{$_}));
    }
    return @no_val;
}


__END__


####------------------------END OF PROGRAM--------------------------####
#                           ==============                             #
####----------------------------------------------------------------####


####--------------------POD-text starts here: ----------------------####
#                       =====================                          #
#  (You can try to read it as it is or convert it into a nicer format  #
#  with one of the programs pod2html, pod2man or pod2latex, that are   #
#  normally part of a Perl-distribution.)                              #

=head1 NAME

PubCrawler - Automated Retrieval of PubMed and GenBank Reports

=head1 SYNOPSIS

     usage: pubcrawler.pl [-add_path] [-c <config_file>]
       [-check] [-copyright] [-d <directory>] [-db <database>] 
       [-extra_query <additional information for query string>]
       [-extra_range <range for extra entries>] [-force_mail] 
       [-format <results format>] [-fullmax <max-docs in full>]
       [-getmax <max-docs to get>] [-h] [-help] [-head <output-header>]
       [-i] [-indent <pixels>] [-l <log_file>] 
       [-lynx <alternative-browser>] [-mail <address for results]   
       [-mail_ascii <address for text-only results]
       [-mail_simple <address for slim HTML results] [-mute] 
       [-n <neighbour_URL>] [-notify <address for notification]
       [-no_test] [-os <operating_system>]
       [-out <output-file>] [-p <proxy_server>] [-pp <proxy_port>]
       [-pauth <proxy_authorization>] [-ppass <proxy_password>]
       [-pre <prefix>] [-q <query_URL>] [-r <retrieve_URL>] 
       [-relentrezdate <relative-entrez-date>] [-retry <number of retries>]
       [-s <search-term] [-spacer <gif>] [-t <timeout>] [-u <test_URL>]
       [-v <verbose>] [-viewdays <view-days>] [-version]

    options:
    -add_path adds the path /cwd/lib to @INC (list of library directories)
              where cwd stands for the current working directory
    -c       configuration file for pubcrawler
    -check   checks if program and additional files are setup correctly
    -copyright shows copyright information
    -d       pubcrawler working directory (config,databas,and output)
    -db      name of database file
    -extra_query additional information for query string (holding, otool, etc.)
    -extra_range specifies the number of documents combined in a link
                  minimum value is 1, defaults to 'fullmax'
    -fullmax maximum number of full length reports shown (per search)
    -getmax  maximum number of documents to retrieve (per search)
    -h       this help message
    -head    HTML-header for output file
    -help    same as -h
    -i       include configuration file in HTML-output
    -indent  indent PubCrawler comments n pixel (default 125)
    -l       name of file for log-information
    -lynx    command for alternative browser
    -mail    e-mail address to send results to
             (optionally append '#' or '@@'  and user name)
    -mail_ascii  e-mail address to send text-only results to
             (optionally append '#' or '@@'  and user name)
    -mail_simple  e-mail address to send slim HTML results to
             (optionally append '#' or '\@\@'  and user name)
    -mute    suppresses messages to STDERR
    -n       URL where neighbourhood searches are directed to
    -notify  e-mail address for notification
         (optionally append '#' or '@@' and user name)
    -no_test skips the proxy-test
    -os      operating system (some badly configured versions of Perl need  
         this to be set explicitly -> 'MacOS', 'Win', and 'Unix')
    -out     name of file for HTML-output
    -p       proxy
    -pp      proxy port
    -pauth   proxy authorization (user name)
    -ppass   proxy password
    -pre     prefix used for default file names (config-file,database,log)
    -q       URL where normal (not neighbourhood) searches are directed to
    -r       specify a URL, that will be used for retrieving
             new hits from NCBI
    -relentrezdate maximum age (relative date of publication in days) 
         of a document to be retrieved
             other valid entries: '1 year','2 years','5 years','10 years','no limit'
    -retry   specify how often PubCrawler should retry queries in case of
             server errors (default = 0)
    -s       search-term ('database#alias#query#')
    -spacer  location of gif that acts as space holder for left column
    -t       timeout (in seconds, defaults to 180)
    -u       test-URL (to test proxy configuration)
    -v       verbose output
    -viewdays number of days each document will be shown
    -version show version of program

Some command-line options can also be set in the configuration file.
If both are set and they conflict, the command-line setting takes priority.

=head1 DESCRIPTION

PubCrawler automates requests for user-specific searches in the PubMed-
and the GenBank-database at NCBI at http://www.ncbi.nlm.nih.gov/ .

=head1 USAGE

=head2 Testing

To test if everything is setup correctly, run the program in B<check-mode> first. At your command prompt, enter the name of the script together with any command line options that might be necessary and the B<-check> option. 

Mac users can do this either by setting the B<check>-variable in their configuration file to '1', or by entering B<-check> at the command line (when the B<prompt>-variable in the configuration file is set to '1', the user will be prompted for command-line options after the start of the program).

Windows users can start the Perl executable in a DOS-box followed by the name of the script, followed by any necessary parameters, including the B<-check> option. 

The program will perform a quick check on all the settings and report any errors that it encountered.

B<Recommended when using the program for the first time!>

=head2 Customization

PubCrawler allows two forms of customization:

=over 4

=item command-line options

For a temporary change of parameters the command-line options as listed in L<SYNOPSIS> can be used.

=item configuration file

Permanent customization of settings can be achieved by editing the PubCrawler configuration file. 

It is divided into three parts: L<Mandatory Variables>, L<Optional Variables> and L<Search Terms>.

The value of any variable can be set by writing the variable name and its value separated by a blank on a single line in the configuration file. The value can consist of several words (eg. Mac-directory-name). Any leading or trailing quotes will be stripped, when the data is read in.

Each search-specification has to be written on one line.
The first word must specify the database (genbank or pubmed).
Any following words enclosed in single quotes (') will be used
as an alias for this query, otherwise they will be considered
Entrez-search-terms, as will the rest of the line.
You must not enclose anything but the alias in single quotes!

A Web-tool is available for comfortable generation of configuration files.
Visit PubCrawler Configurator at http://pubcrawler.gen.tcd.ie/configurator.html

=back

=head2 Automated Execution

PubCrawler makes most sense if you have it started automatically, for example overnight. This would assure that your database is always up to date.

The automation depends on the operating system you are using. In the following a few hints are given how to set up schedules:


=over 4

=item UNIX/LINUX

Unix and Unix-like operating systems normally have the cron-daemon running, which is responsible for the execution of scheduled tasks.

The program B<crontab> lets you maintain your crontab files. To run PubCrawler every night at 1:00 a.m. execute the following command:

    crontab -e

and insert a line like the following:

    0 1 * * * $HOME/bin/PubCrawler.pl

presuming your executable resides in $HOME/bin/PubCrawler.pl. Any command line options can be added as desired.

For more information about crontab type C<man crontab>.

=item WINDOWS 95/98

Windows 98 and the Plus!-pack for Windows 95 come with the ability to schedule tasks.
To set up a schedule for PubCrawler, proceed as follows:

Start the I<Task Scheduler> by clicking on

    Start->Programs->Accessories->System Tools->Scheduled Tasks

Follow the instructions to add or create a new task.
Select the Perl-executable (I<perl.exe>), type in a name for the task and choose 
time and day of start.
Open the advanced properties and edit the I<Run:>-line by adding the name of the script and any command line options. It might look something like the following:

Run: C<C:\perl\bin\perl.exe D:\pubcrawler\PubCrawler.exe -d D:\pubcrawler>

You might also consider entering the working directory for PubCrawler in the I<Start in:>-line.

=item MACINTOSH

Unfortunately the Mac operating system does not have a built in scheduler, but there is a shareware program available called B<Cron for Macintosh>.
It costs $10 and has a web-site at http://gargravarr.cc.utexas.edu/cron/cron.html

=back


=head1 MISCELLANEOUS

=head2 Location of the Configuration File

By default PubCrawler looks for a configuration file named PubCrawler.config (or more precisely, B<I<prefix>.config> if the prefix has been changed by using the C<-pre> command-line option).
The first part of the name (I<prefix>) defaults to the main part of the program name (the basename 
up to the last dot, normally I<PubCrawler>).
Other prefixes can be specified via the command line option C<-pre> followed by a prefix.
Other names for the configuration file can be specified via the command line 
option C<-c> followed by the file name.
Using different prefix names allows multiple uses of the program with multiple output files.

The program will first look for a configuration file in the PubCrawler working directory,
if specified via command line option. The second place to look for it is the
home directory as set in the environmental variable 'HOME'. Last place is
the current working directory. If no configuration file could be found and
not all of the L<Mandatory Variables> are specified via command line options
(see L<SYNOPSIS>) then the program exits with an error message.

=head2 Mandatory Variables

There is a number of variables for which PubCrawler expects values given by the user. These are:

=over 4

=item html_file

name of the ouput-file

=item viewdays

number of days each document will be shown

=item relentrezdate

maximum age (in days) of database entries to be reported

=item getmax 

maximum number of documents to be retrieved for each search carried out

=item fullmax 

the maximum number of documents for which a full report is being presented

=item include_config 

whether or not to append config-file to output

=item search_URL 

URL where standard searches are being carried out
(for neighbourhood searches see next section)

=item retrieve_URL 

URL from which documents are being retrieved


=back


The values for these variables can be specified either in the PubCrawler configuration file or as command-line options (see L<SYNOPSIS>).

=head2 Optional Variables

For some variables an optional value can be set in the configuration file. These are:

=over 4

=item work_dir

working directory for PubCrawler

=item extra_range 

The reports that exceed the number of fully presented articles 
(variable 'fullmax') are incorporated into the results page as links.
The variable extra_range specifies, how many documents are assigned to
each link. The minimum value is 1 and it defaults to the value of 'fullmax'.

=item check

PubCrawler runs in check-mode if set to '1' (see L<Testing>)

=item prompt

PubCrawler prompts Mac-users for command-line options if set to '1'

=item verbose

PubCrawler prints log messages to screen if set to '1'

=item lynx

PubCrawler uses specified command to evoke command-line browser for HTTP-requests
(see also L<Perl-Modules and Alternative Choice>)

=item header

location of header (in HTML-format) that will be used for the output file

=item mail

You can specify an e-mail address to which the results are being sent after
each PubCrawler run. This feature needs the program 'metasend' to work.

=item mail_ascii

You can specify an e-mail address to which the results in text-only format 
are being sent after
each PubCrawler run. This feature needs the program 'metasend' and 'lynx' to work.

=item mail_simple

You can specify an e-mail address to which the results in slim HTML format 
are being sent after
each PubCrawler run. This feature needs the program 'metasend' and 'lynx' to work.

=item notify

You can specify an e-mail address to which a notification will be sent after
each PubCrawler run. It contains the number of new hits and can be personalized
by appending '#' and a name to the address. This feature needs the program 'metasend' to work.

=item neighbour_URL

This is the URL from where ui's for neighbourhood searches (related articles/sequences) are retrieved

=item prefix

alternative prefix for standard files (configuration, database, log)

=item system

explicit assignment of operating system ('MacOS','Win','Unix', or 'Linux')

=item proxy

proxy server for internet connection

=item proxy_port

port of the proxy server, defaults to 80

=item proxy_auth

user name for proxy authorization

=item proxy_pass

pass-word for proxy authorization

B<CAUTION:> Storing passwords in a file represents a possible security risk!
            Use command line option ('-ppass' flag) instead!

=item spacer

Specify the location of a gif that acts as a space holder for the left
column of the results (defaults to http://pubcrawler.gen.tcd.ie/pics/spacer.gif )

=item time_out

time in seconds to wait for internet responses, defaults to 180

=item test_URL

URL for test of proxy-configuration

=item no_test

disables test of proxy-configuration
(chopping of head and tail and collecting UIs)

=item indent

indents PubCrawler comments n pixel (default 125)
to align it with Entrez output

=item format

format for reports from PubMed and GenBank
available options: 
'DocSum','Brief','Abstract','Citation','MEDLINE','ASN.1','ExternalLink','GenBank','FASTA'
(make sure to use one compatible with the databases you are querying!)

=back

=head2 Search Terms

The definition of the search terms is the same as given by I<Entrez Search System>. Please look up their site for more information ( http://www.ncbi.nlm.nih.gov/Entrez/entrezhelp.html#ComplexExpression )

A search-term entered at the command-line (e.g. C<pubcrawler.pl -s 'pubmed#Test-search#Gilbert [AUTH]'>) hides all other queries specified in the configuration script.

=head2 Perl-Modules and Alternative Choice

The HTTP-requests are handled by
Perl-modules, namely B<LWP::Simple>, B<LWP::UserAgent>, and B<HTML::Parser>. These are included in the latest distributions
of Perl for Windows and MacPerl 
(check out http://www.perl.com/pace/pub/perldocs/latest.html ). 
They are also freely available
from the CPAN-archive at http://www.perl.com/CPAN/.

In case you would like to run PubCrawler without these modules you
have to provide a command-line browser as an alternative (like B<lynx>,
available at http://lynx.browser.org ).

To disable the use of the modules comment out the three lines following 
I<#### ADDITIONAL MODULES ####> at the beginning of the file by putting a 'I<#>'
in front of them.
You also have to comment out the line 

    @proxy_config = split /\n/, (get "$proxy_conf");

in the subroutine I<proxy_setting>, somewhere near the middle of the program script (approx. line 2080).

=head2 Setup of Queries

Only queries to the same databases are allowed to share the same alias.
If queries with the same alias but different databases are detected,
their alias will be changed to the query-name. That means that the results
for this entry will appear in a separate section.

Valid database specifiers are:

=over 4

=item pubmed

searches PubMed

=item genbank

searches GenBank

=item pm_neighbour

search PubMed for articles related to a paper specified by PMID

=item gb_neighbour

Currently disabled!
(search GenBank for sequences related to an entry specified by GI)

=back


Please look at the sample configuration file for a varieties of queries.


=head1 REPORTING PROBLEMS

If you have difficulty installing the program or if it doesn't produce
the expected results, please read this documentation carefully and also
check out PubCrawlers web-page at http://www.pubcrawler.ie

If none of this advice helps you should send an e-mail with a detailed 
description of the problem to pubcrawler@tcd.ie

=head1 DOCUMENTATION

=head1 AUTHOR

Original author:  Dr. Ken H. Wolfe, khwolfe@tcd.ie

Second author: Dr. Karsten Hokamp, karsten@oscar.gen.tcd.ie

Both from the Department of Genetics, Trinity College, Dublin

If you have problems, corrections, or questions, please see
L<"REPORTING PROBLEMS"> above.

=head1 REDISTRIBUTION

This is free software, and you are welcome
to redistribute it under conditions of the
GNU General Public License (see file 'COPY' or
http://www.gnu.org/copyleft/gpl.html for more
information).

=head1 HOME PAGE

This program has a home page at http://www.pubcrawler.ie/ .

=head1 LAST MODIFIED

$Id: pubcrawlerApi.pl 12946 2006-08-24 16:37:28Z mheiges $

__END__


changes:
write plain header unless header isn't found


the following can be chosen for the mail file:
type: nothing, notification, results

if (notification) {
    link to results, html-formatting
}
if (results) {
    style: extra links (announcements, pubcrawler sites, contact), css, javascript, form (checkboxes, entrez-box), hyperlinks (full and collapsed articles), html-formatting
    format: brief, summary, xml, medline
}



changes:
- added extra_query parameter to include otools, holdings, etc.
- added reading of new parameters (mail options) from added configuration file and normal configuration file
- replaced profile and query link with Settings link
