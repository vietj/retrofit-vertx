package com.julienviet.retrofit.vertx;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import okhttp3.*;
import okio.Buffer;
import okio.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxCallFactory implements Call.Factory {
    private static final long DEFAULT_TIMEOUT = 10;
    private final HttpClient client;
    private final long timeout;

    public VertxCallFactory(HttpClient client) {
        this(client, DEFAULT_TIMEOUT);
    }

    public VertxCallFactory(HttpClient client, long timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    private static IOException getIOException(Throwable cause) {
        IOException ioe;
        if (cause instanceof IOException) {
            ioe = (IOException) cause;
        } else {
            ioe = new IOException(cause);
        }
        return ioe;
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
                public void onResponse(okhttp3.Call call, Response response) {
                    future.complete(response);
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    future.completeExceptionally(e);
                }
            });
            try {
                return future.get(timeout, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public void enqueue(Callback callback) {
            if (executed.compareAndSet(false, true)) {
                Promise<Response> promise = Promise.promise();
                Future<Response> fut = promise.future();
                fut.onSuccess(ar -> {
                    try {
                        callback.onResponse(this, ar);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                fut.onFailure(cause -> callback.onFailure(this, getIOException(cause)));

                HttpMethod method = HttpMethod.valueOf(retroRequest.method());
                HttpUrl url = retroRequest.url();
                client.request(method, url.port(), url.host(), url.encodedPath(), handler -> {
                    if (handler.failed()) {
                        promise.tryFail(handler.cause());
                        return;
                    }
                    HttpClientRequest request = handler.result();
                    MultiMap headers = request.headers();
                    Map<String, List<String>> origHeaders = retroRequest.headers().toMultimap();
                    for (Map.Entry<String, List<String>> hdr : origHeaders.entrySet()) {
                        headers.add(hdr.getKey(), hdr.getValue());
                    }
                    try {
                        RequestBody body = retroRequest.body();
                        if (body != null && body.contentLength() > 0) {
                            MediaType mediaType = body.contentType();
                            String type;
                            if (mediaType != null) {
                                type = mediaType.toString();
                            } else {
                                type = "application/octet-stream";
                            }
                            request.putHeader("content-type", type);
                            request.putHeader("content-length", Long.toString(body.contentLength()));
                            Buffer buffer = new Buffer();
                            body.writeTo(buffer);
                            request.write(io.vertx.core.buffer.Buffer.buffer(buffer.readByteArray()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    request.exceptionHandler(promise::tryFail);
                    request.response(responseHandler -> {
                        if (responseHandler.failed()) {
                            promise.tryFail(responseHandler.cause());
                            return;
                        }
                        HttpClientResponse resp = responseHandler.result();
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
                            promise.tryComplete(builder.build());
                        });
                    });
                    request.end();
                });
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
        public Timeout timeout() {
            return Timeout.NONE;
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
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
