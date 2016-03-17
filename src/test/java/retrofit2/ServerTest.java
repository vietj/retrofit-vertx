package retrofit2;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Route;
import okhttp3.*;
import okhttp3.Callback;
import okio.Buffer;
import org.junit.Test;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ServerTest {

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
    List<Contributor> contributors(
        @Path("owner") String owner,
        @Path("repo") String repo);
  }

  public class GitHubImpl implements GitHub {


    @Override
    public List<Contributor> contributors(@Path("owner") String owner, @Path("repo") String repo) {
      return Arrays.asList(new Contributor("julien", 10), new Contributor("paulo", 15), new Contributor("clement", 20));
    }
  }

  @Test
  public void theTest() throws Exception {

    // Service instance
    GitHubImpl impl = new GitHubImpl();

    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);

    // Build vertx router from service interface
    Retrofit retrofit = new Retrofit.Builder().baseUrl(API_URL).addCallAdapterFactory(new CallAdapter.Factory() {
      @Override
      public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return new CallAdapter() {
          @Override
          public Type responseType() {
            return ResponseBody.class;
          }
          @Override
          public Object adapt(Call call) {
            throw new UnsupportedOperationException();
          }
        };
      }
    }).build();

    //
    Method javaMethod = GitHub.class.getMethod("contributors", String.class, String.class);
    ServiceMethod.Builder builder = new ServiceMethod.Builder(retrofit, javaMethod);
    ServiceMethod sm = builder.build();
    sm.visit(new ServiceMethod.Visitor() {

      Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
      HttpMethod method;
      String path;
      Map<Integer, Function<HttpServerRequest, Object>> methodParams = new HashMap<Integer, Function<HttpServerRequest, Object>>();

      @Override
      void beginMethod(String httpMethod, HttpUrl baseUrl, String relativeUrl, Headers headers, MediaType contentType, boolean hasBody, boolean isFormEncoded, boolean isMultipart, int length) {
        StringBuilder tmp = new StringBuilder();
        Matcher matcher = pattern.matcher(relativeUrl);
        int prev = 0;
        while (matcher.find()) {
          tmp.append(relativeUrl, prev, matcher.start()).append(':').append(relativeUrl, matcher.start() + 1, matcher.end() - 1);
          prev = matcher.end();
        }
        tmp.append(relativeUrl, prev, relativeUrl.length());
        path = tmp.toString();
        method = HttpMethod.valueOf(httpMethod);
      }
      @Override
      void pathParam(int index, String name) {
        methodParams.put(index, req -> req.getParam(name));
      }
      @Override
      void endMethod() {
        Route route = router.route(method, path);
        route.handler(context -> {
          Object[] args = new Object[javaMethod.getParameterCount()];
          for (int i = 0;i < args.length;i++) {
            Function<HttpServerRequest, Object> mapper = methodParams.get(i);
            if (mapper != null) {
              args[i] = mapper.apply(context.request());
            }
          }
          try {
            GsonConverterFactory factory = GsonConverterFactory.create();
            Converter converter = factory.requestBodyConverter(javaMethod.getReturnType(), null, null, retrofit);
            Object ret = javaMethod.invoke(impl, args);
            RequestBody body = (RequestBody) converter.convert(ret);
            context.response().putHeader("Content-Type", body.contentType().toString());
            Buffer buff = new Buffer();
            body.writeTo(buff);
            context.response().end(buff.snapshot().utf8());
          } catch (Exception e) {
            e.printStackTrace();
            context.response().setStatusCode(500).end();
          }
        });
      }
    });

    // Create vertx router
    vertx.createHttpServer().requestHandler(router::accept).listen(8080, "localhost");

    // Now test with client
    HttpClient client = vertx.createHttpClient();
    CompletableFuture<Void> latch = new CompletableFuture<>();
    client.getNow(8080, "localhost", "/repos/square/retrofit/contributors", resp -> {
      resp.bodyHandler(body -> {
        System.out.println("Got " + body);
      });
      System.out.println("got resp " + resp);
      latch.complete(null);
    });
    latch.get(10, TimeUnit.SECONDS);
    vertx.close();
  }


}
