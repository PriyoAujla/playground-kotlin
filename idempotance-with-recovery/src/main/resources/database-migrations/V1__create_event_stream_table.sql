CREATE TABLE events (
  id SERIAL PRIMARY KEY,
  type text NOT NULL,
  created TIMESTAMP NOT NULL,
  payload JSONB
);