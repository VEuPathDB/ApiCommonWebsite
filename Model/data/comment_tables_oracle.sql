/*
CREATE SCHEMA "comment"
  AUTHORIZATION postgres;
*/


DROP TABLE comment_external_database;
DROP TABLE external_databases;
DROP TABLE locations;
DROP TABLE comments;
DROP TABLE comment_target;
DROP TABLE review_status;

CREATE TABLE comment_target
(
  comment_target_id varchar(20) NOT NULL,
  comment_target_name varchar(200) NOT NULL,
  allow_location NUMBER(1),
  allow_reverse NUMBER(1),
  location_description varchar(4000),
  CONSTRAINT comment_target_key PRIMARY KEY (comment_target_id)
);


CREATE TABLE review_status
(
  review_status_id varchar(20) NOT NULL,
  review_status_name varchar(200) NOT NULL,
  CONSTRAINT review_status PRIMARY KEY (review_status_id)
);

  
CREATE TABLE comments
(
  comment_id NUMBER(10) NOT NULL,
  email varchar(255),
  comment_date date,
  comment_target_id varchar(20),
  stable_id varchar(200),
  conceptual NUMBER(1),
  project_id varchar(200),
  headline varchar(4000),
  content CLOB,
  review_status_id varchar(20),
  CONSTRAINT comments_pkey PRIMARY KEY (comment_id),
  CONSTRAINT comments_ct_id_fkey FOREIGN KEY (comment_target_id)
      REFERENCES comment_target (comment_target_id),
  CONSTRAINT comments_rs_id_fkey FOREIGN KEY (review_status_id)
      REFERENCES review_status (review_status_id)
);


CREATE TABLE external_databases
(
  external_database_id NUMBER(10) NOT NULL,
  external_database_name varchar(200),
  external_database_version varchar(200),
  CONSTRAINT external_databases_pkey PRIMARY KEY (external_database_id)
);


CREATE TABLE locations
(
  comment_id NUMBER(10) NOT NULL,
  location_id NUMBER(10) NOT NULL,
  location_start NUMBER(12),
  location_end NUMBER(12),
  is_reverse NUMBER(1),
  CONSTRAINT locations_pkey PRIMARY KEY (comment_id, location_id),
  CONSTRAINT locations_comment_id_fkey FOREIGN KEY (comment_id)
      REFERENCES comments (comment_id)
);


CREATE TABLE comment_external_database
(
  external_database_id NUMBER(10) NOT NULL,
  comment_id NUMBER(10) NOT NULL,
  CONSTRAINT comment_external_database_pkey PRIMARY KEY (external_database_id, comment_id),
  CONSTRAINT comment_id_fkey FOREIGN KEY (comment_id)
      REFERENCES comments (comment_id),
  CONSTRAINT external_database_id_fkey FOREIGN KEY (external_database_id)
      REFERENCES external_databases (external_database_id)
);


CREATE SEQUENCE comments_pkseq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE locations_pkseq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE external_databases_pkseq START WITH 1 INCREMENT BY 1;


