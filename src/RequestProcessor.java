import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;

public class RequestProcessor implements Runnable {
  private final static Logger logger = Logger.getLogger(
      RequestProcessor.class.getCanonicalName());

  private Socket connection;
  private String serverUrl;
  private File cacheDir;

  public RequestProcessor(Socket connection, File cacheDir, String serverUrl) {
    this.connection = connection;
    this.serverUrl = serverUrl;

    if (cacheDir.isFile()) {
      throw new IllegalArgumentException("cacheDir must be a directory, not a file");
    }

    try {
      cacheDir = cacheDir.getCanonicalFile();
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Cannot get cache directory", ex);
    }

    this.cacheDir = cacheDir;
  }

  @Override
  public void run() {
    try {
      Reader input = new InputStreamReader(new BufferedInputStream(
          connection.getInputStream()),"US-ASCII");
      StringBuilder requestLine = new StringBuilder();

      while (true) {
        int c = input.read();
        if (c == '\r' || c == '\n') break;
        requestLine.append((char) c);
      }

      String get = requestLine.toString();
      String[] tokens = get.split("\\s+");
      String path = tokens[1];
      String filename = path.substring(1, path.length());

      if (path.endsWith("/")) {
        filename += "index.html";
      }

      OutputStream raw = new BufferedOutputStream(connection.getOutputStream());
      Writer out = new OutputStreamWriter(raw);

      byte[] fileFromCache = fetchFromCache(filename);

      if (fileFromCache != null) {
        sendHeader(out, "HTTP/1.0 200 OK", "text/html", fileFromCache.length);
        raw.write(fileFromCache);
        raw.flush();
        return;
      }

      URL u = new URL(this.serverUrl + path);
      HttpURLConnection uc = (HttpURLConnection) u.openConnection();
      uc.setRequestMethod("GET");
      int responseCode = uc.getResponseCode();
      String responseMsg = uc.getResponseMessage();
      logger.info("HTTP/1.x " + responseCode + " " + responseMsg);

      if (responseCode == HttpURLConnection.HTTP_OK) {
        InputStream in = new BufferedInputStream(uc.getInputStream());
        ByteArrayOutputStream outt = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n = 0;

        while (-1!=(n=in.read(buf))) {
          outt.write(buf, 0, n);
        }

        outt.close();
        in.close();
        byte[] response = outt.toByteArray();

        saveInCache(filename, response);

        sendHeader(out, "HTTP/1.0 200 OK", "text/html", response.length);
        raw.write(response);
        raw.flush();

      } else {
        sendHeader(out, "HTTP/1.0 200 OK", "text/html", 23);
        out.write("GET request didn't work");
        logger.severe("GET request didn't work");
      }

      out.flush();
    } catch (IOException ex) {
      logger.log(Level.WARNING,
          "Error talking to " + connection.getRemoteSocketAddress(), ex);
    } finally {
      try {
        connection.close();
      }
      catch (IOException ex) {}
    }
  }

  private void saveInCache(String filename, byte[] content) {
    File theFile = new File(cacheDir, filename);
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(theFile);
      fos.write(content);

    } catch (FileNotFoundException ex) {
      logger.log(Level.WARNING, "Cache file cannot be saved", ex);
    } catch (SecurityException ex) {
      logger.log(Level.WARNING, "Cache file cannot be saved", ex);
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Cache file cannot be saved", ex);
    } finally {
      try {
        if (fos != null) {
          fos.close();
        }
      } catch (IOException ex) {
        logger.log(Level.WARNING, "Cache file output stream cannot be closed", ex);
      }
    }
  }

  private byte[] fetchFromCache(String filename) {
    File file = new File(this.cacheDir, filename);
    String root = this.cacheDir.getPath();
    byte[] data = null;

    try {
      if (file.canRead() && file.getCanonicalPath().startsWith(root)) {
        data = Files.readAllBytes(file.toPath());
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Cannot read cache file", ex);
    }

    return data;
  }

  private void sendHeader(Writer out, String responseCode,
      String contentType, int length)
      throws IOException {
    out.write(responseCode + "\r\n");
    Date now = new Date();
    out.write("Date: " + now + "\r\n");
    out.write("Server: JHTTP 2.0\r\n");
    out.write("Content-length: " + length + "\r\n");
    out.write("Content-type: " + contentType + "; charset=UTF-8\r\n\r\n");
    out.flush();
  }
}
