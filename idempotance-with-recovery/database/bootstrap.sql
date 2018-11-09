CREATE USER playground_user WITH PASSWORD 'playground';

CREATE DATABASE playground;

GRANT ALL ON DATABASE playground TO playground_user;