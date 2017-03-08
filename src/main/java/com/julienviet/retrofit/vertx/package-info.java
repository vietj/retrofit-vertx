/**
 * = Retrofit Vert.x adapter
 *
 * An highly scalable adapter for _Retrofit_.
 *
 * The _Retrofit Vert.x Adapter_ allows to use _Retrofit_ with the Vert.x library.
 *
 * Vert.x is a library that provides an highly scalable HTTP client that features
 *
 * - HTTP/1 or HTTP/2 transport
 * - Connection pooling
 * - SSL / TLS
 * - Proxy support
 * - Non blocking DNS resolution
 * - Native SSL support (OpenSSL, BoringSSL, etc...)
 *
 * == Intro
 *
 * Retrofit turns your HTTP API into a Java interface.
 *
 * [source,java]
 * ----
 * {@link examples.GitHubService}
 * ----
 *
 * The Retrofit class generates an implementation of the GitHubService interface.
 *
 * The {@link com.julienviet.retrofit.vertx.VertxCallFactory} implements Retrofit {@link retrofit2.CallAdapter}
 * delegating to a Vert.x {@link io.vertx.core.http.HttpClient}.
 *
 * [source,java]
 * ----
 * {@link examples.Example#usage()}
 * ----
 *
 * Each Call from the created GitHubService can make a synchronous or asynchronous HTTP request to the remote webserver.
 *
 * [source,java]
 * ----
 * {@link examples.Example#call}
 * ----
 *
 * == Usage
 *
 * [source,xml]
 * ----
 * <dependency>
 *   <groupId>com.julienviet</groupId>
 *   <artifactId>retrofit-vertx</artifactId>
 *   <version>${this.version}</version>
 * </dependency>
 * ----
 *
 * This version is for Retrofit ${retrofit.version} and Vert.x ${vertx.version}
 *
 * == Going asynchronous
 *
 * You can call the `execute` method to perform a blocking http call on the current thread, or you can also enqueue
 * an asynchronous call.
 *
 * [source,java]
 * ----
 * {@link examples.Example#async}
 * ----
 *
 * Vert.x concurrency model is based on the reactor pattern, you can read more at http://vertx.io/docs/vertx-core/java/#_reactor_and_multi_reactor
 *
 * == Using with RxJava
 *
 * Retrofit provides an RxJava adapter you can use, to use it add the adapter library to your build file:
 *
 * [source,xml]
 * ----
 * <dependency>
 *   <groupId>com.squareup.retrofit2</groupId>
 *   <artifactId>adapter-rxjava</artifactId>
 *   <version>2.2.0</version>
 * </dependency>
 * ----
 *
 * using is quite straightforward
 *
 * [source,java]
 * ----
 * Retrofit retrofit = new Retrofit.Builder()
 *   .baseUrl("https://api.github.com/")
 *   .callFactory(new VertxCallFactory(client))
 *   .addCallAdapterFactory(RxJavaCallAdapterFactory.createAsync())
 *   .build();
 *
 * GitHubService service = retrofit.create(GitHubService.class);
 *
 * Single<ResponseBody> single = retrofit.create(GitHubService.class).body();
 * single.subscribe(result -> {
 *   // Callback on Vert.x event loop thread
 * }, error -> {
 *   // Error on Vert.x event loop thread
 * });
 * ----
 *
 * NOTE: there is also an RxJava 2 adapter that works equally well
 *
 * == TLS/SSL configuration
 *
 * Configuring TLS/SSL with a Java truststore is done when creating the client
 *
 * [source,java]
 * ----
 * {@link examples.Example#ssl(io.vertx.core.Vertx)}
 * ----
 *
 * you can also use PKCS12 files
 *
 * [source,java]
 * ----
 * {@link examples.Example#sslWithPKCS12(io.vertx.core.Vertx)} (io.vertx.core.Vertx)}
 * ----
 *
 * or even PEM files
 *
 * [source,java]
 * ----
 * {@link examples.Example#sslWithPEM(io.vertx.core.Vertx)}
 * ----
 *
 * == HTTP/2 support
 *
 * You can configure the client to use HTTP/2 protocol by setting the `alpn` and `protocol` options:
 *
 * [source,java]
 * ----
 * {@link examples.Example#http2(io.vertx.core.Vertx)}
 * ----
 *
 * You need also to configure ALPN for your JVM, you should http://vertx.io/docs/vertx-core/java/#ssl
 *
 * == Proxy support
 *
 * You can configure the client to use a _HTTP/1.x CONNECT_, _SOCKS4a_ or _SOCKS5_ proxy.
 *
 * [source,java]
 * ----
 * {@link examples.Example#proxy(io.vertx.core.Vertx)}
 * ----
 *
 * To know more about proxy support, you should read you should read http://vertx.io/docs/vertx-core/java/#_using_a_proxy_for_client_connections
 *
 */
@Document(fileName = "index.adoc")
package com.julienviet.retrofit.vertx;

import io.vertx.docgen.Document;