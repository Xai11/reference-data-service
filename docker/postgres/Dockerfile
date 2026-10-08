FROM postgres:16

ENV POSTGRES_DB=reference_data \
    POSTGRES_USER=postgres \
    POSTGRES_PASSWORD=postgres

# Скрипты инициализации (выполняются только при первом создании тома)
COPY init/ /docker-entrypoint-initdb.d/

EXPOSE 5432
