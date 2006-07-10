CREATE SCHEMA "comment"
  AUTHORIZATION jerric;
  
  
CREATE TABLE "comment".comments
(
  comment_id int4 NOT NULL,
  email varchar(255),
  comment_date date,
  comment_type varchar(100),
  stable_id varchar(200),
  sequence_type varchar(100),
  conceptual bool,
  project_id varchar(200),
  headline varchar(4000),
  content text,
  review_status varchar(100),
  CONSTRAINT comments_pkey PRIMARY KEY (comment_id)
);


CREATE TABLE "comment".external_databases
(
  external_database_id int4 NOT NULL,
  external_database_name varchar(200),
  external_database_version varchar(200),
  CONSTRAINT external_databases_pkey PRIMARY KEY (external_database_id)
);


CREATE TABLE "comment".locations
(
  comment_id int4 NOT NULL,
  location_id int4 NOT NULL,
  start_min int4,
  start_max int4,
  end_min int4,
  end_max int4,
  CONSTRAINT locations_pkey PRIMARY KEY (comment_id, location_id),
  CONSTRAINT locations_comment_id_fkey FOREIGN KEY (comment_id)
      REFERENCES "comment".comments (comment_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


CREATE TABLE "comment".comment_external_database
(
  external_database_id int4 NOT NULL,
  comment_id int4 NOT NULL,
  CONSTRAINT comment_external_database_pkey PRIMARY KEY (external_database_id, comment_id),
  CONSTRAINT comment_external_database_comment_id_fkey FOREIGN KEY (comment_id)
      REFERENCES "comment".comments (comment_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT comment_external_database_external_database_id_fkey FOREIGN KEY (external_database_id)
      REFERENCES "comment".external_databases (external_database_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);
