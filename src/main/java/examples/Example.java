package examples;

import com.julienviet.retrofit.vertx.VertxCallFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Example {

  public void usage() {

    Vertx vertx = Vertx.vertx();
    HttpClient client = vertx.createHttpClient();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .callFactory(new VertxCallFactory(client))
        .build();

    GitHubService service = retrofit.create(GitHubService.class);

    // Use the service as you would do normally
  }

  public void call(GitHubService service) {
    Call<List<Repo>> repos = service.listRepos("octocat");
  }

  public void async(Call<List<Repo>> repos) {
    repos.enqueue(new Callback<List<Repo>>() {
      @Override
      public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
        // Response is on Vert.x event loop thread
      }

      @Override
      public void onFailure(Call<List<Repo>> call, Throwable t) {
        // Failure is on Vert.x event loop thread
      }
    });
  }

  public void ssl(Vertx vertx) {

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
        .setSsl(true)
        .setTrustStoreOptions(
            new JksOptions()
            .setPath("/path/to/truststore")
            .setPassword("the-password")));

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .callFactory(new VertxCallFactory(client))
        .build();

    GitHubService service = retrofit.create(GitHubService.class);
  }

  public void sslWithPKCS12(Vertx vertx) {

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
        .setSsl(true)
        .setPfxTrustOptions(
            new PfxOptions()
                .setPath("/path/to/pfxstore")
                .setPassword("the-password")));
  }

  public void sslWithPEM(Vertx vertx) {

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
        .setSsl(true)
        .setPemTrustOptions(
            new PemTrustOptions()
                .addCertPath("/path/to/pem1")
                .addCertPath("/path/to/pem2")
                .addCertPath("/path/to/pem3")));
  }

  public void http2(Vertx vertx) {

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
        .setUseAlpn(true)
        .setProtocolVersion(HttpVersion.HTTP_2)
        .setSsl(true)
        .setTrustStoreOptions(
            new JksOptions()
                .setPath("/path/to/truststore")
                .setPassword("the-password")));

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .callFactory(new VertxCallFactory(client))
        .build();

    GitHubService service = retrofit.create(GitHubService.class);
  }

  public void proxy(Vertx vertx) {

    HttpClient client = vertx.createHttpClient(new HttpClientOptions()
        .setProxyOptions(new ProxyOptions()
            .setType(ProxyType.SOCKS5)
            .setHost("localhost")
            .setPort(1080)
            .setUsername("username")
            .setPassword("secret")));

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .callFactory(new VertxCallFactory(client))
        .build();

    GitHubService service = retrofit.create(GitHubService.class);
  }
}
