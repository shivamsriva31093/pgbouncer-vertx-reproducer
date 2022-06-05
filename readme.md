# Read Me

### Start Postgres Container

`docker run -p 5432:5432 -e POSTGRES_PASSWORD=test --name postgres  postgres`

### Start PgBouncer Container

> Start pgbouncer instance in transaction mode

`docker run -p 6432:6432 -e"POSTGRESQL_HOST=$(ifconfig -u | grep 'inet ' | grep -v 127.0.0.1 | cut -d\  -f2 | head -1)" -e "POSTGRESQL_USERNAME=postgres" -e "POSTGRESQL_PASSWORD=test" -e "POSTGRESQL_DATABASE=postgres" -e "PGBOUNCER_POOL_MODE=transaction" -e "PGBOUNCER_PORT=6432" --name=pgbouncer bitnami/pgbouncer`

#### Issues With Transaction Mode
[Prepared Statement and Transaction Pool Mode](https://ledenyi.com/2020/05/greenplums-pgbouncer-and-prepared-statements/)

### To Test

`POST http://localhost:8888/api/v1/test`
