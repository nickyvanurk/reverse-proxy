import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class ReverseProxyServer {
  private static final Logger logger = Logger.getLogger(
      ReverseProxyServer.class.getCanonicalName());
  private final int port;

  public ReverseProxyServer(int port) throws IOException {
    this.port = port;
  }

  public void start() throws IOException {
    logger.log(Level.INFO, "Reverse proxy server starting");
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
