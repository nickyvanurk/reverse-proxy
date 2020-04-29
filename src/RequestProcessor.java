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
  private boolean cacheEnabled;
  private File cacheDir;

  public RequestProcessor(Socket connection, File cacheDir, Boolean cacheEnabled, String serverUrl) {
    this.connection = connection;
    this.serverUrl = serverUrl;
    this.cacheEnabled = cacheEnabled;

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
      String[] headers = getHeaders(connection);
      String filePath = headers[1];

      if (filePath.endsWith("/")) {
        filePath += "index.html";
      }

      byte[] content = fetchFile(filePath);

      OutputStream raw = new BufferedOutputStream(connection.getOutputStream());
      Writer out = new OutputStreamWriter(raw);

      if (content != null) {
        String contentType = URLConnection.getFileNameMap().getContentTypeFor(filePath);
        sendHeader(out, "HTTP/1.0 200 OK", contentType, content.length);
      } else {
        content = "File not found".getBytes("UTF-8");
        sendHeader(out, "HTTP/1.0 404 File Not Found", "text/html; charset=utf-8", content.length);
      }

      raw.write(content);
      raw.flush();
      connection.close();
    } catch (IOException ex) {
      logger.log(Level.WARNING, "Error talking to " + connection.getRemoteSocketAddress(), ex);
    } finally {
      try {
        connection.close();
      }
      catch (IOException ex) {
        logger.log(Level.WARNING, "Can't close connection", ex);
      }
    }
  }

  private byte[] fetchFile(String filePath) {
    if (cacheEnabled) {
      byte[] fileFromCache = fetchFromCache(filePath);

      if (fileFromCache != null) {
        logger.info("Fetching successfully from cache");
        return fileFromCache;
      }

      logger.info("Not in cache, fetching from server");
    }

    byte[] fileFromServer = fetchFromServer(filePath);

    if (fileFromServer != null) {
      if (cacheEnabled) {
        saveInCache(filePath, fileFromServer);
      }

      return fileFromServer;
    }

    logger.log(Level.WARNING, "Can't fetch file from server");
    return null;
  }

  private byte[] fetchFromServer(String filePath) {
    try {
      URL url = new URL(this.serverUrl + filePath);
      HttpURLConnection serverConnection = (HttpURLConnection) url.openConnection();
      int responseCode = serverConnection.getResponseCode();
      String responseMsg = serverConnection.getResponseMessage();

      logger.info("HTTP/1.x " + responseCode + " " + responseMsg);

      if (responseCode == HttpURLConnection.HTTP_OK) {
        InputStream inputStream = new BufferedInputStream(serverConnection.getInputStream());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        int byteRead = 0;

        while ((byteRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, byteRead);
        }

        inputStream.close();
        outputStream.close();

        return outputStream.toByteArray();
      }
    } catch (IOException ex) {
      logger.log(Level.WARNING, filePath + " cannot be fetched from server", ex);
    }

    return null;
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

  private void saveInCache(String filename, byte[] content) {
    File file = new File(cacheDir, filename);
    FileOutputStream fos = null;

    try {
      file.getParentFile().mkdirs();
      fos = new FileOutputStream(file);
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

  private static String[] getHeaders(Socket connection) {
    try {
      Reader input = new InputStreamReader(new BufferedInputStream(
          connection.getInputStream()),"UTF-8");
      StringBuilder requestLine = new StringBuilder();

      while (true) {
        int c = input.read();
        if (c == '\r' || c == '\n') break;
        requestLine.append((char) c);
      }

      return requestLine.toString().split("\\s+");
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Cannot read connection headers");
      throw new NullPointerException("requestLine cannot be null");
    }
  }

  private static void sendHeader(Writer out, String responseCode,
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
