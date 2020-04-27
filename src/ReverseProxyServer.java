import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;


public class ReverseProxyServer {
  private static final Logger logger = Logger.getLogger(
      ReverseProxyServer.class.getCanonicalName());
  private static final int NUM_THREADS = 50;
  private final int port;
  private ArrayList<String> serverUrls;
  private int serverUrlIndex;

  public ReverseProxyServer(int port) throws IOException {
    this.port = port;
    this.serverUrls = new ArrayList<String>();
    this.serverUrls.add("http://localhost:1337");
    this.serverUrls.add("http://localhost:1338");
    this.serverUrls.add("http://localhost:1339");
    this.serverUrls.add("http://localhost:1340");
    this.serverUrlIndex = 0;
  }

  public void start() throws IOException {
    ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);

    try (ServerSocket server = new ServerSocket(port)) {
      logger.info("Accepting connections on port " + server.getLocalPort());

      while (true) {
        try {
          Socket request = server.accept();

          logger.info("Starting new thread");

          Runnable r = new RequestProcessor(request, serverUrls.get(serverUrlIndex));

          serverUrlIndex = (serverUrlIndex + 1) % serverUrls.size();

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
