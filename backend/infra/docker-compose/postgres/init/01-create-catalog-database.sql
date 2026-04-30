SELECT 'CREATE DATABASE asc_moda_catalog'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'asc_moda_catalog'
)\gexec
