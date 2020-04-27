import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.*;

public class RequestProcessor implements Runnable {
  private final static Logger logger = Logger.getLogger(
      RequestProcessor.class.getCanonicalName());

  private String serverUrl;
  private Socket connection;

  public RequestProcessor(Socket connection) {
    this.serverUrl = "http://localhost:1337";
    this.connection = connection;
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

      URL u = new URL(this.serverUrl + path);
      HttpURLConnection uc = (HttpURLConnection) u.openConnection();
      uc.setRequestMethod("GET");
      int responseCode = uc.getResponseCode();
      String responseMsg = uc.getResponseMessage();
      logger.info("HTTP/1.x " + responseCode + " " + responseMsg);

      OutputStream raw = new BufferedOutputStream(
                          connection.getOutputStream()
                         );
      Writer out = new OutputStreamWriter(raw);

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
