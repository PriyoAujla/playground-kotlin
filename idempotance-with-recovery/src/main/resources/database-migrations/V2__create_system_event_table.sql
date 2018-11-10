CREATE TABLE system_events (
  id uuid NOT NULL,
  type text NOT NULL,
  created TIMESTAMP NOT NULL,
  attempt SMALLINT NOT NULL,
  PRIMARY KEY(id, attempt)
);

