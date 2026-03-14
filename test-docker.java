import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

public class Test {
  public static void main(String[] args) {
    System.out.println(com.github.dockerjava.core.DockerClientConfig.createDefaultConfigBuilder().build().getApiVersion());
  }
}
