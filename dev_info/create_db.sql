CREATE DATABASE timakan
WITH OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'fr_CA.utf8'
    LC_CTYPE = 'fr_CA.utf8'
    TABLESPACE = pg_default
    TEMPLATE template0
    CONNECTION LIMIT = -1;
alter database timakan set time zone 'America/Montreal';