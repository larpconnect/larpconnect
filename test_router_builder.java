import io.vertx.core.Vertx;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ResponseValidator;

public class test_router_builder {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        OpenAPIContract.from(vertx, "server/src/main/resources/openapi.yaml").onComplete(ar -> {
            if (ar.succeeded()) {
                RouterBuilder builder = RouterBuilder.create(vertx, ar.result());
                System.out.println("Router builder created.");
                OpenAPIRoute route = builder.getRoute("getMessage");
                System.out.println("route doValidation:" + route.doValidation());
            } else {
                ar.cause().printStackTrace();
            }
            vertx.close();
        });
    }
}
