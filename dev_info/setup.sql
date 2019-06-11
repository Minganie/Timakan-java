CREATE EXTENSION postgis
    VERSION "2.5.0";

----------------------- WATER STATIONS -----------------------
CREATE TABLE water_stations (
    serial          integer primary key, 
    name            text not null unique,
    geom            geometry(POINT,4326) not null
);
GRANT SELECT on water_stations TO timakan_ro;
GRANT INSERT, UPDATE, DELETE on water_stations to timakan_rw;
GRANT SELECT, INSERT, UPDATE on water_stations to timakan_stations_editor;

CREATE TABLE logger_type(
    ltype text primary key
);
GRANT SELECT ON logger_type TO timakan_ro;
GRANT INSERT, UPDATE, DELETE ON logger_type TO timakan_rw;
GRANT SELECT ON logger_type TO timakan_stations_editor;
INSERT INTO logger_type VALUES ('Niveau'), ('Pression');

CREATE TABLE water_station_loggers(
    station integer references water_stations(serial),
    logger integer not null,
    logger_type text not null references logger_type(ltype),
    primary key (station, logger)
);
GRANT SELECT ON water_station_loggers TO timakan_ro;
GRANT INSERT, UPDATE, DELETE ON water_station_loggers TO timakan_rw;
GRANT SELECT, INSERT, UPDATE ON water_station_loggers TO timakan_stations_editor;

----------------------- CORRECTED -----------------------
CREATE TABLE corrected (
    id      serial primary key, 
    ls_id   integer,
    station integer not null REFERENCES water_stations(serial), 
    moment  TIMESTAMP WITH TIME ZONE not null,  
    level   double precision, 
    l_temp  double precision,
    pressure double precision,
    p_temp  double precision,
    corrected double precision,
    UNIQUE (station, moment)
);
CREATE OR REPLACE FUNCTION timakan_corrected()
  RETURNS trigger AS
$BODY$
BEGIN
    IF OLD.level IS NOT NULL AND NEW.pressure IS NOT NULL THEN
        NEW.corrected := OLD.level - (NEW.pressure*0.101972);
    ELSIF NEW.level IS NOT NULL AND OLD.pressure IS NOT NULL THEN
        NEW.corrected := NEW.level - (OLD.pressure*0.101972);
    END IF;
RETURN NEW;
END
$BODY$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS timakan_corrected_fill ON corrected;
CREATE TRIGGER timakan_corrected_fill
BEFORE UPDATE ON corrected
FOR EACH ROW
EXECUTE PROCEDURE timakan_corrected();

GRANT SELECT ON corrected TO timakan_ro;
GRANT INSERT, UPDATE, DELETE on corrected to timakan_rw;
GRANT USAGE ON corrected_id_seq TO timakan_ro;

----------------------- OTHER STATIONS -----------------------
-- Weather
CREATE TABLE weather_publishers (
    name    text PRIMARY KEY
);
GRANT SELECT on weather_publishers TO timakan_ro;
GRANT INSERT, UPDATE, DELETE on weather_publishers to timakan_rw;
GRANT SELECT ON weather_publishers TO timakan_stations_editor;
INSERT INTO weather_publishers VALUES ('Info-Climat');
INSERT INTO weather_publishers VALUES ('Weather Underground');
INSERT INTO weather_publishers VALUES ('Environnement Canada');

CREATE TABLE weather_stations (
    serial          text primary key, 
    name            text not null unique,
    altitude        integer,
    publisher       text not null REFERENCES weather_publishers (name),
    url             text,
    geom            geometry(POINT,4326) not null
);

GRANT SELECT on weather_stations TO timakan_ro;
GRANT INSERT, UPDATE, DELETE on weather_stations to timakan_rw;
GRANT SELECT, INSERT, UPDATE on weather_stations to timakan_stations_editor;

CREATE OR REPLACE FUNCTION timakan_check_url_for_env_can()
  RETURNS trigger AS
$BODY$
BEGIN
    IF NEW.publisher = 'Environnement Canada' AND NEW.url IS NULL
        THEN RAISE EXCEPTION 'A weather station with publisher Environnement Canada must have a non null url.';
    END IF;
RETURN NEW;
END
$BODY$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS timakan_check_url ON weather_stations;
CREATE TRIGGER timakan_check_url
BEFORE INSERT ON weather_stations
FOR EACH ROW
EXECUTE PROCEDURE timakan_check_url_for_env_can();

-- Cameras
CREATE TABLE camera_stations (
    serial          serial primary key, 
    name            text not null unique,
    geom            geometry(POINT,4326) not null
);
GRANT SELECT on camera_stations TO timakan_ro;
GRANT INSERT, UPDATE, DELETE on camera_stations to timakan_rw;
GRANT SELECT, INSERT, UPDATE on camera_stations to timakan_stations_editor;

-- Tides
DROP TABLE IF EXISTS tide_stations;
CREATE TABLE tide_stations (
    serial          serial primary key, 
    name            text not null unique,
    url             text not null,
    geom            geometry(POINT,4326) not null
);
GRANT SELECT on tide_stations TO timakan_ro;
GRANT INSERT, UPDATE, DELETE on tide_stations to timakan_rw;
GRANT SELECT, INSERT, UPDATE on tide_stations to timakan_stations_editor;

-- FUNCTION
CREATE OR REPLACE FUNCTION timakan_get_logger_type(stn integer, logr integer)
  RETURNS TEXT AS
$BODY$
DECLARE
    ltype text;
BEGIN
    SELECT logger_type INTO STRICT ltype FROM water_station_loggers WHERE station=$1 AND (logger=$2+20000000 OR logger=$2+10000000);
    RETURN ltype;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE EXCEPTION 'Can''t find type for logger % at station %', $2, $1;
        WHEN TOO_MANY_ROWS THEN
            RAISE EXCEPTION 'Found more than one logger % at station % (this is really not supposed to happen)', $2, $1;
END;
$BODY$ LANGUAGE 'plpgsql';

-- VIEWS
create view stats as
select station, to_char(moment AT TIME ZONE 'America/Montreal', 'DDD') as od, 
	min(corrected) as q0, 
	PERCENTILE_CONT(0.10) WITHIN GROUP (ORDER BY corrected) as q10,
	PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY corrected) as q50,
	PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY corrected) as q90,
	max(corrected) as q100
from corrected
group by station, to_char(moment AT TIME ZONE 'America/Montreal', 'DDD')
order by station, to_char(moment AT TIME ZONE 'America/Montreal', 'DDD');
GRANT SELECT on stats to timakan_ro;

DROP VIEW IF EXISTS timakan_historic;
CREATE VIEW timakan_historic AS (
select station,
    to_char(date_trunc('week', moment AT TIME ZONE 'America/Montreal') at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') as moment,
    avg(corrected) as corrected
from corrected
WHERE corrected IS NOT NULL
group by station, to_char(date_trunc('week', moment AT TIME ZONE 'America/Montreal') at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"')
order by to_char(date_trunc('week', moment AT TIME ZONE 'America/Montreal') at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"'));
GRANT SELECT on timakan_historic TO timakan_ro;

CREATE VIEW timakan_year AS (
with moy as (
    select station, 
        date_trunc('day', moment AT TIME ZONE 'America/Montreal') as moment, 
        avg(corrected) as corrected
    from corrected as co
    WHERE co.moment > NOW() - interval '1YEAR' and corrected is not null
    group by station, date_trunc('day', moment AT TIME ZONE 'America/Montreal')
)
select co.station, 
    to_char(moment at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') as moment, 
    corrected, 
    q0, 
    q10, 
    q50, 
    q90, 
    q100
from moy as co
	join stats as s on co.station = s.station and to_char(co.moment AT TIME ZONE 'America/Montreal', 'DDD') = s.od
order by co.station, to_char(moment at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"')
);
GRANT SELECT on timakan_year TO timakan_ro;

CREATE VIEW timakan_week AS (
select co.station, 
    to_char(moment at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"') AS moment, 
    corrected, 
    q0, 
    q10, 
    q50, 
    q90, 
    q100
from corrected as co
	join stats as s on co.station = s.station and to_char(co.moment AT TIME ZONE 'America/Montreal', 'DDD') = s.od
where moment > now() - interval '2WEEKS' and corrected is not null
);
GRANT SELECT on timakan_week TO timakan_ro;

-- UPDATE CSVs for Highcharts
CREATE OR REPLACE FUNCTION timakan_export_historic(station integer)
    RETURNS VOID
    LANGUAGE plpgsql
    SECURITY DEFINER
    AS $BODY$
    DECLARE
        file_path text := '/var/www/html/csv/' || station::text || '_all.csv';
    BEGIN
        EXECUTE '
            COPY
                (select moment, corrected from timakan_historic where station='||$1||')
            TO
                ' || quote_literal(file_path) || '
            WITH (
                FORMAT CSV, HEADER
            );
        ';
    END;
$BODY$;
CREATE OR REPLACE FUNCTION timakan_export_year(station integer)
    RETURNS VOID
    LANGUAGE plpgsql
    SECURITY DEFINER
    AS $BODY$
    DECLARE
        file_path text := '/var/www/html/csv/' || station::text || '_year.csv';
    BEGIN
        EXECUTE '
            COPY
                (select moment, corrected, q0, q10, q50, q90, q100 from timakan_year where station='||$1||')
            TO
                ' || quote_literal(file_path) || '
            WITH (
                FORMAT CSV, HEADER
            );
        ';
    END;
$BODY$;
CREATE OR REPLACE FUNCTION timakan_export_week(station integer)
    RETURNS VOID
    LANGUAGE plpgsql
    SECURITY DEFINER
    AS $BODY$
    DECLARE
        file_path text := '/var/www/html/csv/' || station::text || '_week.csv';
    BEGIN
        EXECUTE '
            COPY
                (select moment, corrected, q0, q10, q50, q90, q100 from timakan_week where station='||$1||')
            TO
                ' || quote_literal(file_path) || '
            WITH (
                FORMAT CSV, HEADER
            );
        ';
    END;
$BODY$;

-- Don't let just anyone do this privileged thing
REVOKE ALL ON FUNCTION timakan_export_historic(station integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION timakan_export_historic(station integer) TO timakan_exporter;
REVOKE ALL ON FUNCTION timakan_export_year(station integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION timakan_export_year(station integer) TO timakan_exporter;
REVOKE ALL ON FUNCTION timakan_export_week(station integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION timakan_export_week(station integer) TO timakan_exporter;