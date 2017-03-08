package retrofit2;

import io.netty.handler.codec.TooLongFrameException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.retrofit.VertxRetrofit;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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

  public interface Abc {
    @PUT("/foo/bar/") //
    Call<ResponseBody> sendBody(@Body RequestBody body);
  }

  Vertx vertx;
  HttpClient client;
  Retrofit retrofit;

  @Before
  public void setUp() throws Exception {
    vertx = Vertx.vertx();
    client = vertx.createHttpClient();
    retrofit = new Retrofit.Builder()
        .callFactory(new VertxRetrofit(client))
        .baseUrl(API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build();
  }

  @After
  public void tearDown(TestContext ctx) {
    vertx.close(ctx.asyncAssertSuccess());
  }

  @Test
  public void testAsync(TestContext ctx) throws Exception {
    startHttpServer();
    Call<List<Contributor>> asyncCall = retrofit.create(GitHub.class).contributors("square", "retrofit");
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
    startHttpServer();
    Call<List<Contributor>> syncCall = retrofit.create(GitHub.class).contributors("square", "retrofit");
    List<Contributor> contributors = syncCall.execute().body();
    assertEquals(30, contributors.size());
  }

  @Test
  public void testConnectError(TestContext ctx) throws Exception {
    Call<List<Contributor>> asyncCall = retrofit.create(GitHub.class).contributors("square", "retrofit");
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

  @Test
  public void testResponseError(TestContext ctx) throws Exception {
    startHttpServer(req -> {
      NetSocket so = req.netSocket();
      so.write("HTTP/1.1 200 OK\r\n");
      so.write("Transfer-Encoding: chunked\r\n");
      so.write("\r\n");
      so.write("0\r\n"); // Empty chunk

      // Send large trailer
      for (int i = 0;i < 2000;i++) {
        so.write("01234567");
      }
    });
    Call<List<Contributor>> asyncCall = retrofit.create(GitHub.class).contributors("square", "retrofit");
    Async async = ctx.async();
    asyncCall.enqueue(new retrofit2.Callback<List<Contributor>>() {
      @Override
      public void onResponse(Call<List<Contributor>> call, retrofit2.Response<List<Contributor>> response) {
        ctx.fail();
      }
      @Override
      public void onFailure(Call<List<Contributor>> call, Throwable throwable) {
        ctx.assertEquals(throwable.getCause().getClass(), TooLongFrameException.class);
        async.complete();
      }
    });
  }

  @Test
  public void sendBody(TestContext ctx) throws Exception {
    Async async = ctx.async();
    startHttpServer(req -> {
      req.bodyHandler(buff -> {
        ctx.assertEquals("hello world", buff.toString());
        req.response().end();
        async.complete();
      });
    });
    Call<ResponseBody> asyncCall = retrofit.create(Abc.class).sendBody(RequestBody.create(MediaType.parse("text/plain"), "hello world"));
    asyncCall.execute();
  }

  private void startHttpServer() throws Exception {
    startHttpServer(req -> {
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

  private void startHttpServer(Handler<HttpServerRequest> handler) throws Exception {
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
