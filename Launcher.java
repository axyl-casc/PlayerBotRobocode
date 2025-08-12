import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Launcher {
    public static void main(String[] args) {
        // If Robocode Tank Royale supplies the server URL and secret on the
        // command line, skip the UI and start the bot immediately.
        if (args.length >= 2) {
            new PlayerBot(args[0], args[1]).start();
            return;
        }

        Frame frame = new Frame("PlayerBot Launcher");
        frame.setLayout(new BorderLayout());

        Panel inputPanel = new Panel(new GridLayout(3, 2));
        Label urlLabel = new Label("Server address:");
        TextField urlField = new TextField("ws://localhost:7654");
        Label secretLabel = new Label("Server secret:");
        TextField secretField = new TextField("");
        Button connectButton = new Button("Connect");
        Label statusLabel = new Label("");

        inputPanel.add(urlLabel);
        inputPanel.add(urlField);
        inputPanel.add(secretLabel);
        inputPanel.add(secretField);
        inputPanel.add(connectButton);
        inputPanel.add(statusLabel);
        frame.add(inputPanel, BorderLayout.NORTH);

        TextArea logArea = new TextArea("", 10, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
        logArea.setEditable(false);
        frame.add(logArea, BorderLayout.CENTER);

        frame.pack();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setVisible(true);

        connectButton.addActionListener(e -> {
            connectButton.setEnabled(false);
            statusLabel.setText("Connecting...");
            EventQueue.invokeLater(() ->
                    logArea.append("Connecting to " + urlField.getText() + "\n"));
            new Thread(() -> {
                try {
                    PrintStream ps = new PrintStream(new TextAreaOutputStream(logArea), true);
                    System.setOut(ps);
                    System.setErr(ps);
                    PlayerBot bot = new PlayerBot(urlField.getText(), secretField.getText());
                    EventQueue.invokeLater(() -> logArea.append("Bot started\n"));
                    bot.start();
                    frame.dispose();
                } catch (Exception ex) {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    logArea.append(sw.toString());
                    statusLabel.setText("Error: " + ex.getMessage());
                    connectButton.setEnabled(true);
                }
            }).start();
        });
    }

    private static class TextAreaOutputStream extends OutputStream {
        private final TextArea textArea;

        TextAreaOutputStream(TextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            EventQueue.invokeLater(() -> textArea.append(String.valueOf((char) b)));
        }

        @Override
        public void write(byte[] b, int off, int len) {
            String text = new String(b, off, len);
            EventQueue.invokeLater(() -> textArea.append(text));
        }
    }
}
