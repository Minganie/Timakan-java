CREATE ROLE timakan_ro WITH 
NOSUPERUSER
NOCREATEDB
NOCREATEROLE
INHERIT
NOLOGIN
NOREPLICATION;

CREATE ROLE timakan_rw WITH 
NOSUPERUSER
NOCREATEDB
NOCREATEROLE
INHERIT
NOLOGIN
NOREPLICATION;
GRANT timakan_ro TO timakan_rw;

CREATE ROLE timakan_exporter WITH
NOSUPERUSER
NOCREATEDB
NOCREATEROLE
INHERIT
NOLOGIN
NOREPLICATION;
GRANT timakan_exporter TO postgres;

-- CREATE ROLE timakan_web WITH 
  -- LOGIN
  -- NOSUPERUSER
  -- INHERIT
  -- NOCREATEDB
  -- NOCREATEROLE
  -- NOREPLICATION
  -- PASSWORD 'SECURE_PASSWORD_HERE';
GRANT timakan_ro TO timakan_web;

-- CREATE ROLE timakan_planned_task WITH 
  -- LOGIN
  -- NOSUPERUSER
  -- INHERIT
  -- NOCREATEDB
  -- NOCREATEROLE
  -- NOREPLICATION
  -- PASSWORD 'SECURE_PASSWORD_HERE';
GRANT timakan_rw TO timakan_planned_task;
GRANT timakan_exporter TO timakan_planned_task;

CREATE ROLE timakan_stations_editor WITH
NOSUPERUSER
NOCREATEDB
NOCREATEROLE
INHERIT
NOLOGIN
NOREPLICATION;

-- Create all other water/weather/etc. station editors who will use QGIS similarly
-- Do not remove the two dashes from this line and the line above.
-- CREATE ROLE pbourdon WITH 
  -- LOGIN
  -- NOSUPERUSER
  -- INHERIT
  -- NOCREATEDB
  -- NOCREATEROLE
  -- NOREPLICATION
  -- PASSWORD 'SECURE_PASSWORD_HERE';
GRANT timakan_stations_editor TO pbourdon;