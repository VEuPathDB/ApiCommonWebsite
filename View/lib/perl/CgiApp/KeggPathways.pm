package ApiCommonWebsite::View::CgiApp::KeggPathways;
use base qw( ApiCommonWebsite::View::CgiApp );

use strict;

use SOAP::Lite;

use CGI::Carp qw(fatalsToBrowser set_message);

# ========================================================================
# ----------------------------- BEGIN Block ------------------------------
# ========================================================================
BEGIN {
    # Carp callback for sending fatal messages to browser
    sub handle_errors {
        my ($msg) = @_;
        print "<p><pre>$msg</pre></p>";
    }
    set_message(\&handle_errors);
}

#--------------------------------------------------------------------------------

sub run {
  my ($self, $cgi) = @_;

  my $ec_numbers = $cgi->param('ec_numbers');
  my $projectId = $cgi->param('project_id');
  my @ecNumbers = $ec_numbers =~ /\d+\.\d+\.\d+\.\d+/g;

  unless(scalar @ecNumbers > 0) {
    exit(0);
  }

  my $serv = SOAP::Lite -> service("http://soap.genome.jp/KEGG.wsdl");

  my $pathwayNamesMap = $self->getPathwayNamesMap($serv);
  my $dbEcNumbers = $self->getDbEcNumbers($cgi);

  print STDOUT $cgi->header();
  print STDOUT $cgi->start_html('Kegg Pathway Table'); 

  my $pathways = $serv->get_pathways_by_enzymes(\@ecNumbers);


  my $ecString = join(' ', @ecNumbers);
  print STDOUT $cgi->h1("Kegg Pathways containing: $ecString");
  print STDOUT $cgi->h3("*** Enzymes found in $projectId are Shown in blue");

  print STDOUT "<table>";
  foreach my $pathway (@$pathways) {

    my $pathway_enzymes = $serv->get_enzymes_by_pathway($pathway);

     my @coloredEnzymes ;
     my @fgColors;
     my @bgColors;
     foreach my $enzyme (@$pathway_enzymes) {

       $enzyme =~ s/ec://;
       if($dbEcNumbers->{$enzyme}) {
         push @coloredEnzymes, "ec:$enzyme";
         push @fgColors, 'white';
         push @bgColors, 'blue';
       }

     }

    my $result = $serv->get_html_of_colored_pathway_by_objects($pathway, \@coloredEnzymes, \@fgColors, \@bgColors);  

    print STDOUT "<tr><td><a href='$result'>" . $pathwayNamesMap->{$pathway} . "</a></td></tr>";
 }
  print STDOUT "</table>";


  print STDOUT $cgi->end_html();
  exit(0);
}


#--------------------------------------------------------------------------------

 sub SOAP::Serializer::as_ArrayOfstring{
   my ($self, $value, $name, $type, $attr) = @_;
   return [$name, {'xsi:type' => 'array', %$attr}, $value];
 }

 sub SOAP::Serializer::as_ArrayOfint{
   my ($self, $value, $name, $type, $attr) = @_;
   return [$name, {'xsi:type' => 'array', %$attr}, $value];
 }

#--------------------------------------------------------------------------------

 sub getPathwayNamesMap {
   my ($self, $serv) = @_;

   my $allPathways = $serv->list_pathways('map');

   my %pathwayNames;
   foreach my $pathway (@$allPathways) {
     my $name = $pathway->{entry_id};
     my $def =  $pathway->{definition};
#     print "$name\t$def\n";
     $pathwayNames{$name} = $def;
   }
   return \%pathwayNames;
 }

#--------------------------------------------------------------------------------

sub getDbEcNumbers {
  my ($self, $cgi) = @_;

  my $dbh = $self->getQueryHandle($cgi);
  my $sql = "select ec_numbers from webready.GeneAttributes_p where ec_numbers is not null";
  my $sh = $dbh->prepare($sql);
  $sh->execute();

  my %all_ecs;

  while(my ($ec) = $sh->fetchrow_array()) {
    my @a = $ec =~ /\d+\.\d+\.\d+\.\d+/g;
    foreach(@a) {
      $all_ecs{$_} = 1;
    }
  }

  $sh->finish();
  return \%all_ecs;
}


1;
