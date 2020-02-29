import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ConnectionHandler implements AutoCloseable {

    private static final String prefix = "A";

    private SSLSocket conn;
    private BufferedReader in;
    private BufferedWriter out;
    private int counter;
    private boolean debug;

    ConnectionHandler(String host, int port, boolean debug) throws IOException {

        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        this.conn = (SSLSocket) sslSocketFactory.createSocket(host, port);
        this.in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
        this.counter = 0;
        this.debug = debug;

        conn.startHandshake();
        recv("*");
    }

    private String tag() {
        return prefix + counter;
    }

    @Override
    public void close() throws IOException {

        conn.close();
        in.close();
        out.close();
    }

    private void send(String message) throws IOException {

        String fullMessage = MessageFormat.format("{0}{1} {2}\r\n", prefix, ++counter, message);
        out.write(fullMessage);
        out.flush();

        if (debug) System.out.println(fullMessage);
    }

    private List<String> recv(String token) throws IOException {

        List<String> lines = new ArrayList<>();
        String str;

        do {

            str = in.readLine();
            lines.add(str);

            if (debug) System.out.println(str);

        } while (!str.contains(token));

        return lines;
    }


    private boolean isSuccessful(List<String> response) {

        int len = response.size();
        String rc = response.remove(len - 1);
        return rc.contains("OK");
    }

    private void command(String cmd) throws IOException {

        send(cmd);
        recv(tag());
    }

    private List<String> request(String cmd) throws IOException {

        send(cmd);
        return recv(tag());
    }


    void login(String username, String password) throws IOException {
        command(MessageFormat.format("LOGIN {0} {1}", username, password));
    }

    List<String> list() throws IOException {

        List<String> folders = new ArrayList<>();

        List<String> lines = request("LIST \"\" *");
        if (isSuccessful(lines)) {

            for (String line : lines) {

                String[] arr = line.split("\"");
                String folder = arr[arr.length - 1];
                folders.add(folder);

                if (debug) System.out.println(folder);
            }
        }

        return folders;
    }

    boolean select(String folder) throws IOException {
        return isSuccessful(request(MessageFormat.format("SELECT \"{0}\"", folder)));
    }

    int[] search() throws IOException {

        List<String> response = request("SEARCH ALL");
        if (isSuccessful(response)) {

            return Arrays.stream(response.get(0).split(" ")).skip(2).mapToInt(Integer::parseInt).toArray();

        } else throw new IOException("Fatal Error -- search() unsuccessful");
    }

    List<String> fetchHeader(int email) throws IOException {

        List<String> response = request(MessageFormat.format("FETCH {0} (BODY[HEADER.FIELDS (from subject)])", email));

        String subject = response.get(1).replaceFirst("Subject: ", "");
        subject = subject.replaceAll("[^A-Za-z0-9]", "-");

        String emailAddress = response.get(2);
        emailAddress = emailAddress.substring(emailAddress.indexOf("<") + 1, emailAddress.indexOf(">"));

        return Arrays.asList(subject, emailAddress);
    }

    List<String> fetchBody(int email) throws IOException {

        List<String> response = request(MessageFormat.format("FETCH {0} BODY[TEXT]", email));
        if (isSuccessful(response)) {

            return response.subList(1, response.size());

        } else throw new IOException("Fatal Error -- fetchBody() unsuccessful");
    }

    void delete(int email) throws IOException {
        command(MessageFormat.format("STORE {0} +FLAGS (\\Deleted)", email));
    }

    void expunge() throws IOException {
        command("CLOSE");
    }

    void logout() throws IOException {
        command("LOGOUT");
    }
}
