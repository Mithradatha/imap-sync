import org.apache.commons.cli.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

class Client {

    private static final String VERSION_NUMBER = "2.6";
    private static final boolean DEBUG_MODE = false;

    public static void main(String[] args) {

        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();

        /* Command-Line Options */
        Option host = Option.builder("S").longOpt("server")
                .desc("target server").hasArg().argName("server-name").build();
        Option harbor = Option.builder("P").longOpt("port")
                .desc("target port").hasArg().argName("port-number").build();
        Option username = Option.builder("l").longOpt("login")
                .desc("username").hasArg().argName("username").build();
        Option password = Option.builder("p").longOpt("pass")
                .desc("password").hasArg().argName("password").build();
        Option folder = Option.builder("f").longOpt("folder")
                .desc("download from folder").hasArg().argName("folder-name").build();

        Option delete = new Option("d", "delete", false, "delete after downloading");
        Option all = new Option("a", "all", false, "download all folders");
        Option help = new Option("h", "help", false, "usage manual");
        Option version = new Option("v", "version", false, "version number");


        options.addOption(host);
        options.addOption(harbor);
        options.addOption(username);
        options.addOption(password);
        options.addOption(folder);
        options.addOption(delete);
        options.addOption(all);
        options.addOption(help);
        options.addOption(version);

        //imap_download.sh -S <server> -P <port> -l <login> [-p <password>] -d -a -f <folder>

        try {

            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("v")) {

                System.out.println("imap_download version " + '"' + VERSION_NUMBER + '"');
                System.out.println();
            }
            if (cmd.hasOption("h")) {

                formatter.printHelp("imap_download", options);
                System.out.println();
            }

            String server = cmd.getOptionValue("S");
            int port = Integer.parseInt(cmd.getOptionValue("P"));
            String user = cmd.getOptionValue("l");
            String pass = cmd.getOptionValue("p");

            boolean deleteAll = cmd.hasOption("d");
            boolean downloadAll = cmd.hasOption("a");

            List<String> requestedFolders = new ArrayList<>();

            if (!downloadAll && cmd.hasOption("f")) {

                Option[] ops = cmd.getOptions();

                for (Option op : ops) {
                    if (op.getOpt().equals("f")) {
                        requestedFolders.add(op.getValue());
                    }
                }

                if (DEBUG_MODE) requestedFolders.forEach(System.out::println);
            }

            try (ConnectionHandler handle = new ConnectionHandler(server, port, DEBUG_MODE)) {

                handle.login(user, pass);

                List<String> directories = handle.list();
                if (!requestedFolders.isEmpty()) directories.removeIf(dir -> !requestedFolders.contains(dir));

                int count = 0;

                String cwd = System.getProperty("user.dir");
                String root = String.valueOf(Files.createDirectories(Paths.get(cwd + File.separator + "IMAP")));

                for (String directory : directories) {

                    String folderPath = root + File.separator + directory;
                    Files.createDirectories(Paths.get(folderPath));

                    if (handle.select(directory)) {

                        int[] emails = handle.search();

                        for (int email : emails) {

                            List<String> header = handle.fetchHeader(email);
                            String title = header.get(0);
                            String sender = header.get(1);

                            Path emailPath = Files.createDirectories(Paths.get(folderPath + File.separator +
                                    MessageFormat.format("{0}_{1}_{2}", count++, sender, title)));

                            List<String> body = handle.fetchBody(email);

                            Files.write(Paths.get(emailPath + File.separator + "content.txt"), body);

                            if (deleteAll) handle.delete(email);
                        }

                        handle.expunge();
                    }
                }

                handle.logout();

            } catch (IOException e) {
                System.out.println(e.getMessage());

                if (DEBUG_MODE) e.printStackTrace();
            }

        } catch (ParseException exp) {
            System.out.println("Invalid Parameters -- " + exp.getMessage());
        }
    }
}
