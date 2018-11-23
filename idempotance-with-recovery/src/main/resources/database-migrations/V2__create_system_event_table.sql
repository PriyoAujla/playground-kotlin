CREATE TABLE transactor_events (
  id text NOT NULL,
  type text NOT NULL,
  attempt SMALLINT NOT NULL,
  transaction_id uuid NOT NULL,
  created TIMESTAMP NOT NULL
  PRIMARY KEY(id, `type`, attempt)
);
