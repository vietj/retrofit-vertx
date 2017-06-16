package com.julienviet.retrofit.vertx;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxCallFactory implements Call.Factory {

  private final HttpClient client;

  public VertxCallFactory(HttpClient client) {
    this.client = client;
  }

  private class VertxCall implements okhttp3.Call {

    private final Request retroRequest;
    private final AtomicBoolean executed = new AtomicBoolean();

    VertxCall(Request retroRequest) {
      this.retroRequest = retroRequest;
    }

    @Override
    public Request request() {
      return retroRequest;
    }

    @Override
    public Response execute() throws IOException {
      CompletableFuture<Response> future = new CompletableFuture<>();
      enqueue(new Callback() {

        @Override
        public void onResponse(okhttp3.Call call, Response response) throws IOException {
          future.complete(response);
        }

        @Override
        public void onFailure(okhttp3.Call call, IOException e) {
          future.completeExceptionally(e);
        }
      });
      try {
        return future.get(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new IOException(e);
      }
    }

    @Override
    public void enqueue(Callback callback) {
      if (executed.compareAndSet(false, true)) {
        Future<Response> fut = Future.future();
        fut.setHandler(ar -> {
          if (ar.succeeded()) {
            try {
              callback.onResponse(this, ar.result());
            } catch (IOException e) {
              // WTF ?
              e.printStackTrace();
            }
          } else {
            IOException ioe;
            Throwable cause = ar.cause();
            if (cause instanceof IOException) {
              ioe = (IOException) cause;
            } else {
              ioe = new IOException(cause);
            }
            callback.onFailure(this, ioe);
          }
        });
        HttpMethod method = HttpMethod.valueOf(retroRequest.method());
        HttpClientRequest request = client.requestAbs(method, this.retroRequest.url().toString(), resp -> {
          resp.exceptionHandler(fut::tryFail);
          resp.bodyHandler(body -> {
            Response.Builder builder = new Response.Builder();
            builder.protocol(Protocol.HTTP_1_1);
            builder.request(this.retroRequest);
            builder.code(resp.statusCode());
            builder.message(resp.statusMessage());
            for (Map.Entry<String, String> header : resp.headers()) {
              builder.addHeader(header.getKey(), header.getValue());
            }
            String mediaTypeHeader = resp.getHeader("Content-Type");
            MediaType mediaType = mediaTypeHeader != null ? MediaType.parse(mediaTypeHeader) : null;
            builder.body(ResponseBody.create(mediaType, body.getBytes()));
            fut.tryComplete(builder.build());
          });
        });
        request.exceptionHandler(fut::tryFail);
        int size = retroRequest.headers().size();
        Headers retroHeaders = retroRequest.headers();
        MultiMap headers = request.headers();
        for (int i = 0;i < size;i++) {
          String header = retroHeaders.name(i);
          String value = retroHeaders.value(i);
          headers.add(header, value);
        }
        try {
          RequestBody body = this.retroRequest.body();
          if (body != null && body.contentLength() > 0) {
            request.putHeader("content-length", "" + body.contentLength());
            Buffer buffer = new Buffer();
            body.writeTo(buffer);
            request.write(io.vertx.core.buffer.Buffer.buffer(buffer.readByteArray()));
          }
        } catch (IOException e) {
          e.printStackTrace(); // ?
        }
        request.end();
      } else {
        callback.onFailure(this, new IOException("Already executed"));
      }
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean isExecuted() {
      return executed.get();
    }

    @Override
    public boolean isCanceled() {
      return false;
    }

    @Override
    public Call clone() {
      return new VertxCall(retroRequest);
    }
  }

  @Override
  public okhttp3.Call newCall(Request request) {
    return new VertxCall(request);
  }
}
