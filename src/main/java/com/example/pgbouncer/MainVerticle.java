package com.example.pgbouncer;

import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.impl.PgPoolOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.web.Router;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.repackaged.org.apache.commons.lang3.RandomUtils;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class MainVerticle extends AbstractVerticle {

  public static final String INSERT_QUERY = "" +
    "INSERT INTO public.customers (" +
    "customer_id, company_name, contact_name, contact_title, address, city, region, postal_code, country, phone, fax) " +
    "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11) returning *";

  public void runLiquibase() {

    Liquibase liquibase = null;
    Connection c = null;
    try {
      Class.forName("org.postgresql.Driver");
      c = DriverManager.getConnection("jdbc:postgresql://"
          + PROPERTIES.get("database.host") + ":"
          + PROPERTIES.get("database.port") + "/",
        PROPERTIES.get("database.user"),
        PROPERTIES.get("database.password")
      );
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
      liquibase = new Liquibase("./liquibase/changelog-master.xml", new ClassLoaderResourceAccessor(), database);
      liquibase.update("main");
    } catch (SQLException | LiquibaseException e) {
      e.printStackTrace();
      throw new NoSuchElementException(e.getMessage());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } finally {
      if (c != null) {
        try {
          c.rollback();
          c.close();
        } catch (SQLException e) {
          //nothing to do
        }
      }
    }
  }

  public PgPool getPgPoolClient() {
    final PgConnectOptions options = new PgConnectOptions()
      .setHost(PROPERTIES.get("database.host"))
      .setPort(Integer.parseInt(PROPERTIES.get("database.port")))
      .setUser(PROPERTIES.get("database.user"))
      .setPassword(PROPERTIES.get("database.password"))
      .setDatabase(PROPERTIES.get("database.name"))
      .addProperty("application_name", "pgbouncer-test")
      .setConnectTimeout(5 * 60 * 1000)
      .setPipeliningLimit(1)
      .setTracingPolicy(TracingPolicy.PROPAGATE);
    options.setReconnectAttempts(2).setReconnectInterval(1000);
    final PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(8)
      .setName("transactional:pool");
    poolOptions.setEventLoopSize(8);
    return PgPool.pool(vertx, options, new PgPoolOptions(poolOptions));
  }

  public static HashMap<String, String> PROPERTIES = new HashMap<>();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    WorkerExecutor sharedWorkerExecutor = vertx.createSharedWorkerExecutor("db-pool");
    PROPERTIES = Utils.propertiesToMap("application.properties");
    io.vertx.rxjava3.pgclient.PgPool pool = io.vertx.rxjava3.pgclient.PgPool.newInstance(getPgPoolClient());

    runLiquibase();

    Router router = Router.router(Vertx.newInstance(vertx));
    router.route()
      .path("/api/v1/test")
      .method(HttpMethod.POST)
      .produces("application/json")
      .handler(event -> {
        Tuple params = Tuple.tuple()
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt())
          .addString("Dummy" + RandomUtils.nextInt());

        pool
          .preparedQuery(INSERT_QUERY)
          .rxExecute(params)
          .flatMapPublisher(Flowable::fromIterable)
          .map(Row::toJson)
          .toList()
          .flatMapCompletable(res -> event.response().end(new JsonArray(res).encode()))
          .subscribe(() -> {
          }, th -> {
            th.printStackTrace();
            event.fail(new IllegalStateException(th));
          });
      });


    vertx.createHttpServer()
      .requestHandler(router.getDelegate())
      .listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });


  }
}
