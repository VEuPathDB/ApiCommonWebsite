package ApiCommonWebsite::View::CgiApp::DataProviderContactEmail;
@ISA = qw( ApiCommonWebsite::View::CgiApp );

use strict;
use ApiCommonWebsite::View::CgiApp;

sub run {
  my ($self, $cgi) = @_;

  my $projectId = $cgi->param('project_id');
  my $dataset = $cgi->param('ds');

   print STDOUT $cgi->header();
  print STDOUT $cgi->start_html(-title => "Data Set Email",
      );

#  print STDOUT $cgi->header('text/plain');

  my $dbh = $self->getQueryHandle($cgi);

  my $sql = "select dsp.display_name,
       dsp.display_category,
       dsp.type,
       dsp.subtype,
       listagg(dsc.email, ',') WITHIN GROUP (ORDER BY dataset_contact_id) as email,
       listagg(dsc.name, ',') WITHIN GROUP (ORDER BY dataset_contact_id) as contact_name,
       dsp.build_number_introduced,
      listagg(dsnt.name, ',') WITHIN GROUP (ORDER BY dsnt.name) as dataset_name
from apidbtuning.datasetpresenter dsp, apidbtuning.datasetcontact dsc, apidbtuning.datasetnametaxon dsnt
where dsp.dataset_presenter_id = dsc.dataset_presenter_id 
and dsp.dataset_presenter_id = dsnt.dataset_presenter_id 
and (dsp.dataset_name_pattern = ? or dsp.name = ?)
and dsc.email is not null
group by dsp.display_name, dsp.display_category, dsp.type, dsp.subtype, dsp.build_number_introduced";
  
  my $sth = $dbh->prepare($sql);
  $sth->execute($dataset,$dataset);

  while (my ($displayName, $displayCategory, $type, $subtype, $email, $contactName, $buildNumberIntroduced, $datasetNames) = $sth->fetchrow_array) {

    my $dataType ;

    if($displayCategory) {
      $dataType = $displayCategory;
    }
    else {
      $dataType = $type;
      $dataType =~ s/_/ /g;
      $dataType =~ s/\b(\w)/\u$1/g;
    }


    print STDOUT $cgi->h2("Auto Generated Email Message for Contacting Data Providers");

   print STDOUT $cgi->h3("Names and Email");

    print STDOUT $cgi->start_ul();
    my @names = split(',', $contactName);
    my @emails = split(',', $email);

    for(my $i = 0; $i < scalar @names; $i++) {
      my $nameString = $names[$i] . " - " . $emails[$i];
      print STDOUT $cgi->li($nameString);
    }


    print STDOUT $cgi->end_ul();


       print STDOUT $cgi->h3("Subject Line");

    print STDOUT $cgi->start_ul();
    print STDOUT $cgi->li("[EuPathDB bld${buildNumberIntroduced}] ${projectId} ${dataType} Experiment Ready for Review");
    print STDOUT $cgi->end_ul();


       print STDOUT $cgi->h3("Message Body:");

    print STDOUT $cgi->p("Your Data set named \"$displayName\" has been successfully integrated into EuPathDB and is scheduled for immediate release.  Please log in to our password protected qa site to verify the accuracy of the data and related descriptions.  We will not make this data publicly available until you have given us the \"ok\" but please review this as soon as possible so we can make any needed changes in our current release cycle.  If we don\'t hear back or if substantial changes are needed we may choose to move this data set to our next scheduled release (> 6weeks away).");


    print STDOUT $cgi->p("Your Data set(s) can be accessed here:");

    my $link = "http://qa.${projectId}.org/a/getDataset.do?datasets=$datasetNames";

    print STDOUT $cgi->a( {href =>"http://qa.${projectId}.org/a/getDataset.do?datasets=$datasetNames"}, "http://qa.${projectId}.org/a/getDataset.do?datasets=$datasetNames");

    print STDOUT $cgi->p("username:  TODO <br /> password:  TODO");

    print STDOUT $cgi->p("The page above contains all text which is specific to your dataset.  We can easily add descriptions, contacts, or links to external sites which are specific to this dataset.  Please click through the \"Links\" section on this page to ensure they are accurate (links should contain helpful text when you mouseover them).  Some of the links will be to external sites and some will provide specific examples of this data on EuPathDB.  Also notice the \"Searches using these Data\" links.  These links are all EuPathDB internal and allow various ways to mine this data set. Many of these will require you to choose your dataset from a drop down list (ie. They may not be specific to your data set)");

    print STDOUT $cgi->p("Note that the QA site is under active development so please forgive broken links or periods of slowness/lack of connectivity.");

    print STDOUT $cgi->p("Please report everything you find to be broken or confusing.");

    print STDOUT $cgi->p("Thanks in advance,");
    print STDOUT $cgi->p("EuPathDB Team");

    print STDOUT $cgi->p("------------------------------------------------------------------------------------------------");

  }

    print STDOUT $cgi->end_html();
  
  $sth->finish();





}

1;
