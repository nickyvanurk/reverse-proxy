import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.Properties;

public class ReverseProxyServer {
  private static final Logger logger = Logger.getLogger(
      ReverseProxyServer.class.getCanonicalName());
  private static final int NUM_THREADS = 50;
  private final int port;
  private int serverCount;
  private int serverUrlIndex;
  private Properties configFile;

  public ReverseProxyServer(int port) throws IOException {
    this.port = port;
    this.configFile = ReverseProxyServer.readPropertiesFile("../config/config.properties");
    this.serverCount = Integer.parseInt(this.configFile.getProperty("server.count"));
    this.serverUrlIndex = 0;

    for (int i = 0; i < 5; i++) {
      logger.info(this.getServerUrl());
    }
  }

  public void start() throws IOException {
    ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);

    try (ServerSocket server = new ServerSocket(port)) {
      logger.info("Accepting connections on port " + server.getLocalPort());

      while (true) {
        try {
          Socket request = server.accept();

          logger.info("Starting new thread");
          pool.submit(new RequestProcessor(request, this.getServerUrl()));
        } catch (IOException ex) {
          logger.log(Level.WARNING, "Error accepting connection", ex);
        }
      }
    }
  }

  private String getServerUrl() {
    String url = this.configFile.getProperty("server.url" + (this.serverUrlIndex + 1));

    if (url != null) {
      serverUrlIndex = (serverUrlIndex + 1) % serverCount;
    } else {
      logger.warning("Read url is null");
    }

    return url;
  }

  private static Properties readPropertiesFile(String path) {
    Properties configFile = new Properties();

    try {
      configFile.load(new FileInputStream(path));
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error reading config", ex);
    }

    return configFile;
  }

  public static void main(String[] args) {
    int port;
    try {
      port = Integer.parseInt(args[0]);

      if (port < 0 || port > 65535) {
        port = 8080;
      }
    } catch (RuntimeException ex) {
      port = 8080;
    }

    try {
      ReverseProxyServer reverseProxyServer = new ReverseProxyServer(port);
      reverseProxyServer.start();
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Reverse proxy server could not start", ex);
    }
  }
}
