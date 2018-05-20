package game;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

public class Game implements MessageListener{

	private PTPConsumer consumer;
	private PTPProducer producer;

	@FXML
	Label label;
	@FXML
	Label info;
	@FXML
	GridPane board;
	@FXML
	Label opponentLabel;
	@FXML
	Label playerId;

	private int cells[][]; // 0-nic, 1-X, 2-O
	private int currentPlayer;
	private int mySymbol;
	private String myId = "";
	private Integer opponentId;
	private boolean start;
	private boolean myTurn;
	private boolean isEnd;
	
	@FXML
	private void click(MouseEvent e) {
		Label l = (Label) e.getSource();
		
		int row = GridPane.getRowIndex(l);
		int column = GridPane.getColumnIndex(l);
		
		if(myTurn == false || cells[row][column] != 0) return;

		cells[row][column] = currentPlayer;

		if (currentPlayer == 1) {
			l.setText("X");
			currentPlayer = 2;
		} else {
			l.setText("O");
			currentPlayer = 1;
		}
		myTurn = false;
		info.setText("Ruch przeciwnika ...");
		
		producer.sendQueueMessage(""+row+column+myId);
		
		if (checkWon(3 - currentPlayer) == true) { // 3 - currentPlayer bo wyzej przeskakuje tura
			endGame(3 - currentPlayer);
		} else if (checkDraw() == true) {
			endGame(0);
		}
	}

	private void endGame(int who) {
		start = false;

		if(isEnd == false) producer.sendQueueMessage("E"+who+myId);
		isEnd = true;
		
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("GRA ZAKOŃCZONA");
		alert.setHeaderText(null);
		if (who == 0) {
			alert.setContentText("Rozgrywka zakończyła się remisem !");
		} else if (who == 1) {
			alert.setContentText("Rozgrywkę wygrał gracz X !");
		} else {
			alert.setContentText("Rozgrywkę wygrał gracz O !");
		}
		alert.showAndWait();

		restart();
		startConnection();
	}

	private boolean checkWon(int player) {
		for (int i = 0; i < 3; i++) {
			if (cells[0][i] == player && cells[1][i] == player && cells[2][i] == player) {
				return true;
			}
		}
		for (int i = 0; i < 3; i++) {
			if (cells[i][0] == player && cells[i][1] == player && cells[i][2] == player) {
				return true;
			}
		}
		if (cells[0][0] == player && cells[1][1] == player && cells[2][2] == player)
			return true;
		if (cells[0][2] == player && cells[1][1] == player && cells[2][0] == player)
			return true;

		return false;
	}

	private boolean checkDraw() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (cells[i][j] == 0) {
					return false;
				}
			}
		}
		return true;
	}

	private void getId() {
		TextInputDialog dialog = new TextInputDialog("");
		dialog.setTitle("Wybieranie id");
		dialog.setHeaderText(null);
		dialog.setContentText("Podaj swoje id ");
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent())
			myId = result.get();
		try {
			Integer i = Integer.parseInt(myId);
		}
		catch(NumberFormatException nfe) {
			myId = "";
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Nie poprawne id");
			alert.setHeaderText(null);
			alert.setContentText("Podaj liczbe która będzie Twoim id");
			alert.showAndWait();
		}
	}

	@FXML
	private void initialize() {
		while(myId.equals("")) {
			getId();
		}
		
		producer = new PTPProducer(myId);
		consumer = new PTPConsumer(this, myId);
		isEnd = false;
		opponentLabel.setText("Brak połączenia :(");
		playerId.setText("Twoje id="+myId);
		
		restart();
		startConnection();
	}
	
	private void restart() {
		cells = new int[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				cells[i][j] = 0;
			}
		}

		currentPlayer = 1;

		ObservableList<Node> childrens = board.getChildren();
		for (Node node : childrens) {
			if (node.getClass().toString().equals("class javafx.scene.control.Label")) {
				Label lab = (Label) node;
				lab.setText("");
			}
		}
	}

	private void startConnection() {
		if(isEnd == false) { // pierwszy raz sie wykona, pozniej nie bo beda grac ponownie wasEnd = true
			info.setText("Czekanie na rywala ...");
			label.setText("");
			start = false;
			myTurn = false;
		}
		else if(start == false) { // pierwszy ktoremu zamknie sie okienko po rundzie bedzie czekal na drugiego
								  // drugi przez to juz nie przejdzie, bo ustawi sobie start = true po wiadomosci pierwszego
			info.setText("Czekanie na rywala ..");
			label.setText("");
			myTurn = false;
		}
		
		isEnd = false;
		producer.sendQueueMessage("XX" + myId);
	}
	
	public void sendLeaveMessage() {
		if(start == true) 
			producer.sendQueueMessage("RR"+myId); 
		
		consumer.emptyQueue();
		consumer.close();
	}
	
	private void receiveMessage(String message){
		System.out.println("Przyszla wiadomosc '"+message+"'");
		System.out.println("(start = "+start+", myTurn = "+myTurn+", isEnd = "+isEnd+")");

		if(start == false && message.substring(0,2).equals("XX")) { // nie rozpoczeto rozgrywki a dostano wiadomosc o checi rozpoczecia
			opponentId = Integer.parseInt(message.substring(2,message.length()));
			opponentLabel.setText("Połączenie z graczem o id="+opponentId);
			Integer myIdInteger = Integer.parseInt(myId);
			
			if(opponentId > myIdInteger) {
				System.out.println("MOJE ID JEST MNIEJSZE, USTAWIAM SOBIE myTurn=false I symbol=O");
				start = true;
				myTurn = false;
				mySymbol = 2; // O
				currentPlayer = 1; // X
				info.setText("Ruch przeciwnika ...");
				label.setText("Jestes  O");
			}
			else {
				System.out.println("MOJE ID JEST WIEKSZE, USTAWIAM SOBIE myTurn=true I symbol=X");
				start = true;
				myTurn = true;
				mySymbol = 1; // X
				currentPlayer = 1; // X
				info.setText("Wykonaj swój ruch");
				label.setText("Jestes  X");
			}
		}
		else if(start == true && message.substring(0,2).equals("RR")) { // rywal sie rozlaczyl
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("UTRATA POLACZENIA");
			alert.setHeaderText(null);
			alert.setContentText("Przeciwnik o id="+message.substring(2,message.length()) +" sie rozlaczyl");
			alert.showAndWait();
			
			opponentLabel.setText("Brak połączenia :(");
			isEnd = false; // zeby zahaczylo ifa w startConnection()
			restart();
			startConnection();
		} 
		else if(start == true && myTurn == false) { // odbieram ruch przeciwnika (wczesniej musze sprawdzic czy sie nie rozlaczyl)
			Integer row = Integer.parseInt(message.substring(0,1));
			Integer column = Integer.parseInt(message.substring(1,2));
			System.out.println("row="+row+" column="+column);
			if(row<0 || row>2 || column<0 || column>2) return;
			
			ObservableList<Node> childrens = board.getChildren();
			for (Node node : childrens) {
				if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == column) {
					Label l = (Label) node;
					if (currentPlayer == 1) {
						l.setText("X");
					} else {
						l.setText("O");
					}
					break;
				}
			}
			cells[row][column] = currentPlayer;
			
			myTurn = true;
			currentPlayer = mySymbol; // teraz moja tura
			info.setText("Wykonaj swój ruch");
		} 
		else if(isEnd == false && message.substring(0,1).equals("E")) {
			Integer who = Integer.parseInt(message.substring(1,2)); // na drugiej pozycji jest kto wygral (0-remis)
			isEnd = true;
			endGame(who);
		}
	}

	@Override
	public void onMessage(Message message) {
		Platform.runLater(() -> {
			try {
				receiveMessage(message.getStringProperty("MSG"));
			} catch (JMSException e) {
				e.printStackTrace();
			}
		});
	}
}
