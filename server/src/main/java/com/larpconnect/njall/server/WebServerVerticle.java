package com.larpconnect.njall.server;

import com.google.inject.name.Named;
import com.google.protobuf.util.JsonFormat;
import com.larpconnect.njall.proto.Message;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Web server verticle that serves OpenAPI specification. */
@Singleton
final class WebServerVerticle extends AbstractVerticle {
  private static final int DEFAULT_PORT = 8080;
  private static final String DEFAULT_SPEC = "openapi.yaml";
  private final Logger logger = LoggerFactory.getLogger(WebServerVerticle.class);
  private final int port;
  private final String openApiSpec;
  private final Serializer serializer;
  private final Optional<Consumer<Integer>> portListener;
  private final ContractLoader contractLoader;
  private final FileSystemHelper fileSystemHelper;
  private HttpServer server;

  interface Serializer {
    String print(Message message) throws IOException;
  }

  interface ContractLoader {
    Future<OpenAPIContract> load(Vertx vertx, String path);
  }

  interface FileSystemHelper {
    Path createTempFile(String prefix, String suffix) throws IOException;

    void copy(InputStream in, Path target) throws IOException;

    void deleteOnExit(Path path);
  }

  static class DefaultFileSystemHelper implements FileSystemHelper {
    @Override
    public Path createTempFile(String prefix, String suffix) throws IOException {
      return Files.createTempFile(prefix, suffix);
    }

    @Override
    public void copy(InputStream in, Path target) throws IOException {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void deleteOnExit(Path path) {
      path.toFile().deleteOnExit();
    }
  }

  WebServerVerticle() {
    this(DEFAULT_PORT, DEFAULT_SPEC);
  }

  WebServerVerticle(String openApiSpec) {
    this(DEFAULT_PORT, openApiSpec);
  }

  @Inject
  WebServerVerticle(
      @Named("web.port") int port,
      @Named("openapi.spec") String openApiSpec,
      Optional<Consumer<Integer>> portListener) {
    this(
        port,
        openApiSpec,
        m -> JsonFormat.printer().print(m),
        portListener,
        (vx, p) -> OpenAPIContract.from(vx, p),
        new DefaultFileSystemHelper());
  }

  WebServerVerticle(int port, String openApiSpec) {
    this(
        port,
        openApiSpec,
        m -> JsonFormat.printer().print(m),
        Optional.empty(),
        (vx, p) -> OpenAPIContract.from(vx, p),
        new DefaultFileSystemHelper());
  }

  // Constructor for testing
  WebServerVerticle(
      int port,
      String openApiSpec,
      Serializer serializer,
      Optional<Consumer<Integer>> portListener,
      ContractLoader contractLoader) {
    this(port, openApiSpec, serializer, portListener, contractLoader, new DefaultFileSystemHelper());
  }

  WebServerVerticle(
      int port,
      String openApiSpec,
      Serializer serializer,
      Optional<Consumer<Integer>> portListener,
      ContractLoader contractLoader,
      FileSystemHelper fileSystemHelper) {
    this.port = port;
    this.openApiSpec = openApiSpec;
    this.serializer = serializer;
    this.portListener = portListener;
    this.contractLoader = contractLoader;
    this.fileSystemHelper = fileSystemHelper;
  }

  public int actualPort() {
    if (server != null) {
      return server.actualPort();
    }
    return port;
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Path tempFile;
    try (var in = getClass().getClassLoader().getResourceAsStream(openApiSpec)) {
      if (in == null) {
        startPromise.fail(openApiSpec + " not found on classpath");
        return;
      }
      tempFile = fileSystemHelper.createTempFile("openapi", ".yaml");
      fileSystemHelper.copy(in, tempFile);
      fileSystemHelper.deleteOnExit(tempFile);
    } catch (IOException e) {
      startPromise.fail(e);
      return;
    }

    contractLoader
        .load(vertx, tempFile.toAbsolutePath().toString())
        .onFailure(startPromise::fail)
        .onSuccess(contract -> onContractLoaded(contract, startPromise));
  }

  // Visible for testing
  void onContractLoaded(OpenAPIContract contract, Promise<Void> startPromise) {
    var builder = RouterBuilder.create(vertx, contract);
    builder.getRoute("MessageService_GetMessage").addHandler(this::handleGetMessage);

    var router = builder.createRouter();
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(port)
        .onSuccess(server -> onServerStarted(server, startPromise))
        .onFailure(startPromise::fail);
  }

  // Visible for testing
  void onServerStarted(HttpServer server, Promise<Void> startPromise) {
    this.server = server;
    var actualPort = server.actualPort();
    portListener.ifPresent(listener -> listener.accept(actualPort));
    logger.info("HTTP server started on port {}", actualPort);
    startPromise.complete();
  }

  // Package-private for testing
  void handleGetMessage(RoutingContext ctx) {
    var message = Message.newBuilder().setMessageType("Greeting").build();
    try {
      var json = serializer.print(message);
      ctx.json(new JsonObject(json));
    } catch (RuntimeException | IOException e) {
      logger.error("Failed to convert message to JSON", e);
      ctx.fail(e);
    }
  }
}
