package quiz.klient;

import java.io.*;
import java.net.*;

import javax.swing.*;

import quiz.serwer.CzatProtokol;

import java.awt.*;
import java.awt.event.*;
import java.util.*;


public class KlientGUI extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//GUI
  	private JButton polacz, rozlacz;
  	private JPanel panel;
  	private JTextField host, port, wiadomosc;
  	private JTextArea komunikaty;
  	private JList<String> zalogowani;
	private DefaultListModel<String> listaZalogowanych;

	//Klient
	private String nazwaSerwera = "localhost";
	private int numerPortu = 2345;
	private boolean polaczony = false;
	private Klient watekKlienta;

	public KlientGUI(){
		super("Klient");
		setSize(600, 500);

                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                //setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
          	setLayout(new BorderLayout());

		panel = new JPanel(new FlowLayout());
		komunikaty = new JTextArea();
		komunikaty.setLineWrap(true);
		komunikaty.setEditable(false);

		wiadomosc = new JTextField();

		host = new JTextField(nazwaSerwera ,12);
		port = new JTextField((new Integer(numerPortu)).toString() ,8);
		polacz = new JButton("PoĹ‚Ä…cz");
		rozlacz = new JButton("RozĹ‚Ä…cz");
		rozlacz.setEnabled(false);

		listaZalogowanych = new DefaultListModel<String>();
		zalogowani = new JList<String>(listaZalogowanych);
		zalogowani.setFixedCellWidth(120);

          	ObslugaZdarzen obsluga = new ObslugaZdarzen();

		polacz.addActionListener(obsluga);
		rozlacz.addActionListener(obsluga);

		wiadomosc.addKeyListener(obsluga);

		addWindowListener(new WindowAdapter(){
                                        public void windowClosing(WindowEvent e) {
                                            rozlacz.doClick();
                                            setVisible(false);
                                            System.exit(0);
                                        }
                                    });

                panel.add(new JLabel("Serwer: "));
                panel.add(host);
                panel.add(new JLabel("Port: "));
                panel.add(port);
                panel.add(polacz);
                panel.add(rozlacz);

                add(panel, BorderLayout.NORTH);
                add(new JScrollPane(komunikaty), BorderLayout.CENTER);
                add(new JScrollPane(zalogowani), BorderLayout.EAST);
                add(wiadomosc, BorderLayout.SOUTH);

   		setVisible(true);

	}

	private class ObslugaZdarzen extends KeyAdapter implements ActionListener, CzatProtokol {

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("PoĹ‚Ä…cz")) {
				wyswietlKomunikat("Ĺ�Ä…czÄ™ z: " + nazwaSerwera + " na porcie: " + numerPortu + "...");
				polaczony = true;
				polacz.setEnabled(false);
				rozlacz.setEnabled(true);
				host.setEnabled(false);
				port.setEnabled(false);
				watekKlienta = new Klient();
				watekKlienta.start();
				//repaint();
			}
			if (e.getActionCommand().equals("RozĹ‚Ä…cz")){
				watekKlienta.wyslij("",LOGOUT_COMMAND);
				polaczony = false;
				rozlacz.setEnabled(false);
				polacz.setEnabled(true);
				host.setEnabled(true);
				port.setEnabled(true);
				//repaint();
			}
		}

		public void keyReleased(KeyEvent e){
			if(e.getKeyCode() == 10) {
				watekKlienta.wyslij(wiadomosc.getText(), POST_COMMAND);
			}
		}
    }

	private class Klient extends Thread implements CzatProtokol{
		private Socket socket;
		private BufferedReader wejscie;
		private PrintWriter wyjscie;

		public void run(){
		  try {
			socket = new Socket(host.getText(), new Integer(port.getText()));
		  	wyswietlKomunikat("PoĹ‚Ä…czono.");

			wejscie = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			wyjscie = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

			wyslij("", LOGIN_COMMAND);

			String lancuch = null;

			while(polaczony){
				lancuch = wejscie.readLine();

				if(lancuch.startsWith(POST_COMMAND)){
					wyswietlKomunikat(lancuch.substring(POST_COMMAND.length()));
				}
				if(lancuch.startsWith(LOGIN_COMMAND)){
					wyswietlKomunikat(lancuch.substring(LOGIN_COMMAND.length()));

					String nick = JOptionPane.showInputDialog(null, "Podaj nick: ");

					wyslij(nick, NICK_COMMAND);
				}
				if(lancuch.startsWith(NICKLIST_COMMAND)){
					//Aktualizacja listy
					StringTokenizer uzytkownicy = new StringTokenizer(lancuch.substring(NICKLIST_COMMAND.length()), ",");

					listaZalogowanych.clear();
					//while(uzytkownicy.hasMoreTokens())
					//	listaZalogowanych.addElement(uzytkownicy.nextToken());

                                        System.out.println(uzytkownicy.countTokens());
                                        System.out.println(listaZalogowanych);

                                        int iloscOsob = uzytkownicy.countTokens();
                                        for(int i = 0; i < iloscOsob; i++) {
                                            listaZalogowanych.add(i, uzytkownicy.nextToken());
                                        }

                                        System.out.println(listaZalogowanych);
				}
				if(lancuch.startsWith(LOGOUT_COMMAND)){
					wyswietlKomunikat(lancuch.substring(LOGOUT_COMMAND.length()));
					listaZalogowanych.clear();
					polaczony = false;
					rozlacz.setEnabled(false);
					polacz.setEnabled(true);
					host.setEnabled(true);
					port.setEnabled(true);
				}
			}
		  }
		  catch (UnknownHostException e) {
		  	wyswietlKomunikat("BĹ‚Ä…d poĹ‚Ä…czenia!");
		  }
		  catch (IOException e) {
		  	wyswietlKomunikat(e.toString());
		  }
		  finally {
			 try {
				wejscie.close();
				wyjscie.close();
				socket.close();
			}
			catch(IOException e) {
			}
		 }
		}

	  public void wyslij(String tekst, String protokol) {
	  		wyjscie.println(protokol + tekst);
	  		wiadomosc.setText("");
	  }
	}

	private void wyswietlKomunikat(String tekst){
		komunikaty.append(tekst + "\n");
		komunikaty.setCaretPosition(komunikaty.getDocument().getLength());
	}

/*	public static void main (String[] args) {
		new KlientGUI();
	} */
}