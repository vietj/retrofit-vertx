## Retrofit Vert.x adapter

An highly scalable adapter for _Retrofit_ with Vert.x

Based on http://square.github.io/retrofit/ and http://vertx.io.

Supporting

- HTTP/1 or HTTP/2 transport
- Connection pooling
- SSL / TLS
- Proxy support
- Non blocking DNS resolution
- Native SSL support (OpenSSL, BoringSSL, etc…​)

## Usage

To use the adapter, add the following dependency to the _dependencies_ section of your build descriptor:

* Maven (in your `pom.xml`):

```
<dependency>
  <groupId>com.julienviet</groupId>
  <artifactId>retrofit-vertx</artifactId>
  <version>1.0.2</version>
</dependency>
```

* Gradle (in your `build.gradle` file):

```
dependencies {
  compile 'com.julienviet:retrofit-vertx:1.0.2'
}
```

You can read the [Documentation](http://www.julienviet.com/retrofit-vertx/guide/java/index.html).

## Snapshots

[![Build Status](https://travis-ci.org/vietj/retrofit-vertx.svg?branch=master)](https://travis-ci.org/vietj/retrofit-vertx)

Use the dependency

```
<dependency>
  <groupId>com.julienviet</groupId>
  <artifactId>retrofit-vertx</artifactId>
  <version>1.0.3-SNAPSHOT</version>
</dependency>
```

Snapshots are deploy in Sonatype OSS repository: https://oss.sonatype.org/content/repositories/snapshots/com/julienviet/retrofit-vertx/

## License

Apache License - Version 2.0

## Publishing docs

* mvn package -Pdocs
* cp -r target/docs docs/
* mv docs/retrofit-vertx docs/guide
