package GUS::Model::DoTS::RNA_Row;

# THIS CLASS HAS BEEN AUTOMATICALLY GENERATED BY THE GUS::ObjRelP::Generator 
# PACKAGE.
#
# DO NOT EDIT!!

use strict;
use GUS::Model::GusRow;

use vars qw (@ISA);
@ISA = qw (GUS::Model::GusRow);

sub setDefaultParams {
  my ($self) = @_;
  $self->setVersionable(1);
  $self->setUpdateable(1);
}

sub setRnaId {
  my($self,$value) = @_;
  $self->set("rna_id",$value);
}

sub getRnaId {
    my($self) = @_;
  return $self->get("rna_id");
}

sub setDescription {
  my($self,$value) = @_;
  $self->set("description",$value);
}

sub getDescription {
    my($self) = @_;
  return $self->get("description");
}

sub setReviewStatusId {
  my($self,$value) = @_;
  $self->set("review_status_id",$value);
}

sub getReviewStatusId {
    my($self) = @_;
  return $self->get("review_status_id");
}

sub setGeneId {
  my($self,$value) = @_;
  $self->set("gene_id",$value);
}

sub getGeneId {
    my($self) = @_;
  return $self->get("gene_id");
}

sub setReviewerSummary {
  my($self,$value) = @_;
  $self->set("reviewer_summary",$value);
}

sub getReviewerSummary {
    my($self) = @_;
  return $self->get("reviewer_summary");
}

sub setSequenceOntologyId {
  my($self,$value) = @_;
  $self->set("sequence_ontology_id",$value);
}

sub getSequenceOntologyId {
    my($self) = @_;
  return $self->get("sequence_ontology_id");
}

sub setModificationDate {
  my($self,$value) = @_;
  $self->set("modification_date",$value);
}

sub getModificationDate {
    my($self) = @_;
  return $self->get("modification_date");
}

sub setUserRead {
  my($self,$value) = @_;
  $self->set("user_read",$value);
}

sub getUserRead {
    my($self) = @_;
  return $self->get("user_read");
}

sub setUserWrite {
  my($self,$value) = @_;
  $self->set("user_write",$value);
}

sub getUserWrite {
    my($self) = @_;
  return $self->get("user_write");
}

sub setGroupRead {
  my($self,$value) = @_;
  $self->set("group_read",$value);
}

sub getGroupRead {
    my($self) = @_;
  return $self->get("group_read");
}

sub setGroupWrite {
  my($self,$value) = @_;
  $self->set("group_write",$value);
}

sub getGroupWrite {
    my($self) = @_;
  return $self->get("group_write");
}

sub setOtherRead {
  my($self,$value) = @_;
  $self->set("other_read",$value);
}

sub getOtherRead {
    my($self) = @_;
  return $self->get("other_read");
}

sub setOtherWrite {
  my($self,$value) = @_;
  $self->set("other_write",$value);
}

sub getOtherWrite {
    my($self) = @_;
  return $self->get("other_write");
}

sub setRowUserId {
  my($self,$value) = @_;
  $self->set("row_user_id",$value);
}

sub getRowUserId {
    my($self) = @_;
  return $self->get("row_user_id");
}

sub setRowGroupId {
  my($self,$value) = @_;
  $self->set("row_group_id",$value);
}

sub getRowGroupId {
    my($self) = @_;
  return $self->get("row_group_id");
}

sub setRowProjectId {
  my($self,$value) = @_;
  $self->set("row_project_id",$value);
}

sub getRowProjectId {
    my($self) = @_;
  return $self->get("row_project_id");
}

sub setRowAlgInvocationId {
  my($self,$value) = @_;
  $self->set("row_alg_invocation_id",$value);
}

sub getRowAlgInvocationId {
    my($self) = @_;
  return $self->get("row_alg_invocation_id");
}

1;
