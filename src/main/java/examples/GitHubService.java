package examples;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface GitHubService {
  @GET("/repos/{owner}/repos")
  Call<List<Repo>> listRepos(@Path("user") String owner);
}
