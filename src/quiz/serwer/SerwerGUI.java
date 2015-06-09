package quiz.serwer;

import java.io.*;
import java.net.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class SerwerGUI extends JFrame {

    //GUI
    private JButton uruchom, zatrzymaj;
    private JPanel panel;
    private JTextField port;
    private JTextArea komunikaty;
    //Serwer
    private int numerPortu = 2345;
    private boolean uruchomiony = false;
    private Vector<Polaczenie> klienci = new Vector<Polaczenie>();

    public SerwerGUI() {
        super("Serwer");
        setSize(350, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        panel = new JPanel(new FlowLayout());
        komunikaty = new JTextArea();
        komunikaty.setLineWrap(true);
        komunikaty.setEditable(false);

        port = new JTextField((new Integer(numerPortu)).toString(), 8);
        uruchom = new JButton("Uruchom");
        zatrzymaj = new JButton("Zatrzymaj");
        zatrzymaj.setEnabled(false);

        ObslugaZdarzen obsluga = new ObslugaZdarzen();

        uruchom.addActionListener(obsluga);
        zatrzymaj.addActionListener(obsluga);

        panel.add(new JLabel("Port: "));
        panel.add(port);
        panel.add(uruchom);
        panel.add(zatrzymaj);

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(komunikaty), BorderLayout.CENTER);

        setVisible(true);
    }

    private class ObslugaZdarzen implements ActionListener {

        private Serwer srw;

        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Uruchom")) {
                srw = new Serwer();
                srw.start();
                uruchomiony = true;
                uruchom.setEnabled(false);
                zatrzymaj.setEnabled(true);
                port.setEnabled(false);
                repaint();
            }
            if (e.getActionCommand().equals("Zatrzymaj")) {
                srw.kill();
                uruchomiony = false;
                zatrzymaj.setEnabled(false);
                uruchom.setEnabled(true);
                port.setEnabled(true);
                repaint();
            }
        }
    }

    private class Serwer extends Thread implements CzatProtokol {


		private ServerSocket server;

        public void kill() {
            try {
                server.close();
                for (Polaczenie klient : klienci) {
                    try {
                        klient.wyjscie.println(LOGOUT_COMMAND + "Serwer przestał działać!");
                        klient.socket.close();
                    } catch (IOException e) {
                    }
                }
                wyswietlKomunikat("Wszystkie Połączenia zostały zakończone.\n");
            } catch (IOException e) {
            }
        }

        public void run() {

            try {

                server = new ServerSocket(new Integer(port.getText()));
                wyswietlKomunikat("Serwer uruchomiony na porcie: " + port.getText() + "\n");

                while (uruchomiony) {
                    Socket socket = server.accept();
                    wyswietlKomunikat("Nowe połączenie.\n");
                    new Polaczenie(socket).start();
                }
            } catch (SocketException e) {
            } catch (Exception e) {
                wyswietlKomunikat(e.toString());
            } finally {
                try {
                    if (server != null) {
                        server.close();
                    }
                } catch (IOException e) {
                    wyswietlKomunikat(e.toString());
                }
            }
            wyswietlKomunikat("Serwer zatrzymany.\n");
        }
    }

    private class Polaczenie extends Thread implements CzatProtokol {

        private BufferedReader wejscie;
        private PrintWriter wyjscie;
        private Socket socket;
        private String nick;
        private boolean polaczony;

        public Polaczenie(Socket w) {
            socket = w;
            polaczony = true;

            synchronized (klienci) {
                klienci.add(this);
            }
        }

        public void run() {

            try {
                wejscie = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                wyjscie = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                while (uruchomiony && polaczony) {

                    String lancuch = wejscie.readLine();

                    if (lancuch.startsWith(POST_COMMAND)) {
                        for (Polaczenie klient : klienci) {
                            klient.wyjscie.println(POST_COMMAND + "<" + nick + "> " + lancuch.substring(POST_COMMAND.length()));
                        }
                    }
                    if (lancuch.startsWith(NICK_COMMAND)) {
                        //sprawdzenie poprawności nicka

                        nick = lancuch.substring(NICK_COMMAND.length()).trim();

                        if (nick.equals("null") || nick.trim().equals("")) {
                            wyjscie.println(LOGIN_COMMAND + "Podaj poprawny nick!");
                            continue;
                        }

                        boolean prawidlowy = true;

                        for (Polaczenie klient : klienci) {
                            if (klient.equals(this) && klient != this) {
                                prawidlowy = false;
                            }
                        }

                        if (!prawidlowy) {
                            wyjscie.println(LOGIN_COMMAND + "Taki nick już jest na czacie! Podaj inny.");
                            continue;
                        }

                        StringBuilder lista = new StringBuilder();

                        for (Polaczenie klient : klienci) {
                            lista.append(klient.nick + ",");
                            klient.wyjscie.println(POST_COMMAND + "Użytkownik " + nick + " dołączył do czatu");
                        }
                        for (Polaczenie klient : klienci) {
                            klient.wyjscie.println(NICKLIST_COMMAND + lista.toString());
                        }
                    }
                    if (lancuch.startsWith(LOGIN_COMMAND)) {
                        wyjscie.println(LOGIN_COMMAND + "Witaj na serwerze!\n");
                    }
                    if (lancuch.startsWith(LOGOUT_COMMAND)) {

                        wyjscie.println(LOGOUT_COMMAND + "Żegnaj.\n");

                        synchronized (klienci) {
                            klienci.remove(this);
                        }
                        StringBuilder lista = new StringBuilder();

                        for (Polaczenie klient : klienci) {
                            lista.append(klient.nick + ",");
                            klient.wyjscie.println(POST_COMMAND + "Użytkownik " + nick + " opuścił czat.");
                        }
                        for (Polaczenie klient : klienci) {
                            klient.wyjscie.println(NICKLIST_COMMAND + lista.toString());
                        }
                        polaczony = false;

                    }

                }
                wyswietlKomunikat("Połączenie zostało zakończone.\n");
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                try {
                    wejscie.close();
                    wyjscie.close();
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        public boolean equals(Polaczenie p) {
            return (p.nick.equals(nick));
        }
    }

    private void wyswietlKomunikat(String tekst) {
        komunikaty.append(tekst);
        komunikaty.setCaretPosition(komunikaty.getDocument().getLength());
    }

    public static void main(String[] args) {

        new SerwerGUI();
    }
}