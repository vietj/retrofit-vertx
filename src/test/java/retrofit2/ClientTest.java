package retrofit2;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.retrofit.VertxRetrofit;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class ClientTest {

  public static final String API_URL = "http://localhost:8080";

  public static class Contributor {
    public final String login;
    public final int contributions;

    public Contributor(String login, int contributions) {
      this.login = login;
      this.contributions = contributions;
    }
  }

  public interface GitHub {
    @GET("/repos/{owner}/{repo}/contributors")
    Call<List<Contributor>> contributors(
        @Path("owner") String owner,
        @Path("repo") String repo);
  }

  Vertx vertx;
  GitHub github;
  HttpClient client;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    client = vertx.createHttpClient();
    Retrofit retrofit = new Retrofit.Builder()
        .callFactory(new VertxRetrofit(client))
        .baseUrl(API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build();
    github = retrofit.create(GitHub.class);
  }

  @After
  public void tearDown(TestContext ctx) {
    vertx.close(ctx.asyncAssertSuccess());
  }

  @Test
  public void testAsync(TestContext ctx) throws Exception {
    startServer();
    Call<List<Contributor>> asyncCall = github.contributors("square", "retrofit");
    Async async = ctx.async();
    asyncCall.enqueue(new retrofit2.Callback<List<Contributor>>() {
      @Override
      public void onResponse(Call<List<Contributor>> call, retrofit2.Response<List<Contributor>> response) {
        ctx.assertEquals(30, response.body().size());
        async.complete();
      }
      @Override
      public void onFailure(Call<List<Contributor>> call, Throwable throwable) {
        ctx.fail(throwable);
      }
    });
  }

  @Test
  public void testSync() throws Exception {
    startServer();
    Call<List<Contributor>> syncCall = github.contributors("square", "retrofit");
    List<Contributor> contributors = syncCall.execute().body();
    assertEquals(30, contributors.size());
  }

  @Test
  public void testConnectError(TestContext ctx) throws Exception {
    Call<List<Contributor>> asyncCall = github.contributors("square", "retrofit");
    Async async = ctx.async();
    asyncCall.enqueue(new retrofit2.Callback<List<Contributor>>() {
      @Override
      public void onResponse(Call<List<Contributor>> call, retrofit2.Response<List<Contributor>> response) {
        ctx.fail();
      }
      @Override
      public void onFailure(Call<List<Contributor>> call, Throwable throwable) {
        async.complete();
      }
    });
  }

  private void startServer() throws Exception {
    startServer(req -> {
      switch (req.path()) {
        case "/repos/square/retrofit/contributors":
          req.response().sendFile("result.json");
          break;
        default:
          req.response().setStatusCode(404).end();
          break;
      }
    });
  }

  private void startServer(Handler<HttpServerRequest> handler) throws Exception {
    HttpServer server = vertx.createHttpServer();
    CompletableFuture<Void> latch = new CompletableFuture<>();
    server.requestHandler(handler).listen(8080, "localhost", ar -> {
      if (ar.succeeded()) {
        latch.complete(null);
      } else {
        latch.completeExceptionally(ar.cause());
      }
    });
    latch.get(10, TimeUnit.SECONDS);
  }


}
