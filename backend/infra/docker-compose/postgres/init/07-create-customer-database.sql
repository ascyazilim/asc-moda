SELECT 'CREATE DATABASE asc_moda_customer'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'asc_moda_customer'
)\gexec
