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
      URL u = new URL(this.serverUrl);
      HttpURLConnection uc = (HttpURLConnection) u.openConnection();
      uc.setRequestMethod("GET");
      int responseCode = uc.getResponseCode();
      String responseMsg = uc.getResponseMessage();
      logger.info("HTTP/1.x " + responseCode + " " + responseMsg);

      Writer out = new OutputStreamWriter(connection.getOutputStream());

      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(
          uc.getInputStream()));
        String inputLine = "";
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }

        in.close();

        writeHeader(out, "HTTP/1.0 200 OK", "text/html", response.toString().length());
        out.write(response.toString());

      } else {
        writeHeader(out, "HTTP/1.0 200 OK", "text/html", 23);
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

  private void writeHeader(Writer out, String responseCode,
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
