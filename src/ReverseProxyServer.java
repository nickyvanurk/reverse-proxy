import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class ReverseProxyServer {
  private static final Logger logger = Logger.getLogger(
      ReverseProxyServer.class.getCanonicalName());
  private static final int NUM_THREADS = 50;
  private final int port;

  public ReverseProxyServer(int port) throws IOException {
    this.port = port;
  }

  public void start() throws IOException {
    ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);

    try (ServerSocket server = new ServerSocket(port)) {
      logger.info("Accepting connections on port " + server.getLocalPort());

      while (true) {
        try {
          Socket request = server.accept();

          // TODO: create requestProcessor class for reverse proxy server [done]

          // start new thread with request [done]
            // send request to server
            // wait for response
            // send response back
            // close connection

          logger.info("Starting new thread");

          Runnable r = new RequestProcessor(request);
          pool.submit(r);
        } catch (IOException ex) {
          logger.log(Level.WARNING, "Error accepting connection", ex);
        }
      }
    }
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
