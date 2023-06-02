package Chess;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.*;

public class ChessGame {
	/*A Chess Game class*/
	public JFrame frame = new JFrame();	//Frame to display the ChessGame
	public JPanel panel = new JPanel();	//Panel to display the Chess Squares
	public static int click1;			//Stores the user first input
	public static ChessSquare[] GUI;	//An array to store the Chess Squares
	public static ChessBoard board;		//A ChessBoard object to handle the logic of the Chess Game
	public static ArrayList<Move> legal;	//A list to store the legal moves of a chess piece
	public static int computerTurn;			//int to store the computer
	public static Computer computer;
	public static int count = 2;
	public static boolean winner;
	public static int difficulty;
	@SuppressWarnings("static-access")
	public ChessGame(int computerTurn,int difficulty){
		this.difficulty = difficulty;
		this.computerTurn = computerTurn;
		winner = false;
		click1 = -1;		//sets var as no clicks
		//rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8
		long currentTime = System.currentTimeMillis();
		//Tests.runTests();
		System.out.println(currentTime - System.currentTimeMillis());
		Tests.Test test = Tests.test20;
		test.runTest();
		board = new ChessBoard(test.fen);	//Creates a new ChessBoard object
		board.displayAttacks();
		
		computer = board.getComputer();
		
		panel.setBorder(BorderFactory.createEmptyBorder(30,30,10,30));	//Creates a border
		panel.setLayout(new GridLayout(8,8));		//Creates an 8*8 grid for the squares
		GUI = new ChessSquare[64];					//ChessSquare array with a memory of 64
		//For loop to create 64 chess squares and add them to the panel
		for(int i=0; i< 64; i++) {
			GUI[i] = new ChessSquare(i);	//Elon Musk
			panel.add(GUI[i]);				//Tesla
		}
		frame.addKeyListener(new KeyListener());	//Adds a key listner for promotion
		frame.add(panel,BorderLayout.CENTER);		//Adds the panel to the frame
		frame.setSize(300,300);						//Sets the size of the new frame
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	//Allows the frame to commit seppuku
		frame.setTitle("Chess");			//I'm a creative mastermind
		frame.pack();						//Don't know what this does
		frame.setVisible(true);				//Makes the frame visible
		update_display();					//Updates the display
	}
	
	public static void update_display() {
		/*ChessGame.upadate_display()-> None
		 * a function that updates every chess square
		 * with the correct piece and color*/
		String address;		//Stores the location of the correct image
		String fen = board.fenString;
		char letter;
		int emptySpaces;
		int count = 0;
		int newPos;
		//For loop to cycle through all 64 squares
		for(int i = 0; i<fen.length(); i++) {
			if (count > 63) break;
			letter = fen.charAt(i);
			//System.out.print(letter);
			emptySpaces = Character.getNumericValue(letter);
			if(emptySpaces <= 8) {
				for(int j = 0; j < emptySpaces; j++) {
                    address = "Chess/Elements/";
					//address = "C:\\Users\\Mason\\Documents\\Java\\Elements\\";
					if(count % 2 == (count/8) %2){
						address += "W";
					}
					else {
						address += "G";
					}
					GUI[count].setIcon(new ImageIcon(address + ".png"));
					count += 1;
				}
				continue;
			}
            address = "Chess/Elements/";
			//address = "C:\\Users\\Mason\\Documents\\Java\\Elements\\";	//Base address
			//If statement to check what column the square is on
			if(count % 2 == (count/8) %2){
				address += "W";
			}
			else {
				address += "G";
			}
			if(board.getPiece(count).color == Constants.WHITE) {
				address += "W";
			}
			address += letter +".png";
			GUI[count].setIcon(new ImageIcon(address));	//Changes the image of the square
			count += 1;
		}
		if(click1 != -1) {
			for(int a = 0;a < legal.size(); a++) {
				newPos = legal.get(a).finish;	//Gets the move
				System.out.print(newPos +" : ");
				System.out.print(board.getPiece(newPos).type+ " : ");
				System.out.print(legal.get(a).type.toString() + ", ");
				//Checks if the square is a legal move
				if(board.getPiece(newPos).isEmpty()) {
                    address = "Chess/Elements/";
					//address = "C:\\Users\\Mason\\Documents\\Java\\Elements\\";
					if(newPos % 2 == (newPos/8) %2){
						address += "W";
					}
					else {
						address += "G";
					}
					address += "D.png";
					GUI[newPos].setIcon(new ImageIcon(address));	//Changes the image of the square
				}
			}
			System.out.println();
		}
		if(board.turn == computerTurn && winner == false) {
			computer_turn();
		}
	}
	
	public static void computer_turn() {
//		Move move = board.computer_move(difficulty);
//		board.make_move(move,true);
//		if(board.is_promote()) {
//			move = board.computer_move(difficulty);
//			board.promote(board.promotingPawn, move.getFinish());
//		}
//		next_turn();
	}
	
	public static void get_click(int pos) {
		/*ChessGame.get_click(int pos) - > None
		 * Function that runs when a ChessSquare is clicked
		 * processes the users click and selects a ChessPiece/Square*/
		//Checks if a promotion is happening
		if(board.is_promote() || board.turn == computerTurn) {
			return;	//Nope
		}
		//Checks if the click is on the correct Chess pieces.
		if(board.getPiece(pos).color == board.turn && !board.getPiece(pos).isEmpty()) {
			click1 = pos;		//Stores the users first click
			legal = new ArrayList<Move>();
			board.piece_moves(click1, Constants.ALL_MOVES, legal);	//Generates all the legal moves for a player
			//System.out.println(legal.size());
			update_display();	//Updates the display
			return;		//The return on your Bitcoin investment
		}
		//Checks if this is the second click
		if(click1 == -1) {
			return;		//Return to sender
		}	
		int coord;	//var to store the location of a legal move
		//for loop to cycle through every legal move
		for(int i = 0; i<legal.size();i++) {
			coord = legal.get(i).finish;		//gets the legal move
			//Checks if the click is a legal move
			if(coord == pos) {
				//makes the move on the board
				board.make_move(legal.get(i), true);
				legal = new ArrayList<Move>();
				//Checks if there is a pawn promoting
				if(board.is_promote()) {
					update_display();	//Updates the display
					break;
				}
				else {
					next_turn();	//next turn
					break;
				}
			}
		}
	}
	public static void promotion(byte type) {
		/*ChessBoard.promotion(int type) -> Void
		 * function that checks if a promotion is happening
		 * then takes the users input and promotes a pawn*/
		//Checks if promotion is happening
		if(board.is_promote()) {
			board.promote(type);		//Promotes the pawn on the board
			next_turn();					//NEXT
		}
	}
	public static void check_Win() {
		/*ChessBoard.check_Win() -> None
		 * function to check if a player has won
		 * the game*/
		int win = board.isWinner();	//Checks if there is a winner
		boolean draw = false;
//		boolean draw = board.check_draw();
//		//checks for no winner
		if(win == Constants.PROGRESS && draw == false) {
			return;
		}
		JFrame w2 = new JFrame();	//Cool frame
		JPanel p2 = new JPanel();	//Cool panel
		JLabel l2 = new JLabel();	//Cool label
		String[] colors = {"White","Black"};	//Colors
		//Checks for a win
		if(win == Constants.WIN) {
			l2 = new JLabel(colors[board.turn] + " is the winner!");	//Displays winner
		}
		//Checks for a draw
		else{
			l2 = new JLabel("It's a draw LMAO!");	//LMAO
		}
		p2.setLayout(new GridLayout(0,1));		//Gibberish
		p2.setBorder(BorderFactory.createLineBorder(Color.black));	//IDk
		p2.add(l2);	//Adds label to panel
		w2.setSize(100,100);	//Big window
		w2.add(p2);				//Adds panel to frame
		w2.setVisible(true);	//I see you
		winner = true;
	}
	public static void next_turn() {
		/*ChessBoard.next_turn()-> None
		 * function to move on to the next
		 * players turn*/
		click1 =-1;	//Resets click variable
		check_Win();		//Checks for a win
		update_display();	//Updates the display
		board.displayAttacks();
//		System.out.println(computer.totalMoves(count));
//		count --;
	}
}
