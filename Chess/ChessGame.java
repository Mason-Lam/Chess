package Chess;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.*;

import Chess.ChessBoard.BoardStorage;

import static Chess.Constants.PieceConstants.*;
import static Chess.Constants.EvaluateConstants.*;

public class ChessGame {
	/*A Chess Game class*/
	private final JFrame frame = new JFrame();	//Frame to display the ChessGame
	private final JPanel panel = new JPanel();	//Panel to display the Chess Squares
	private final ChessSquare[] GUI;	//An array to store the Chess Squares
	private final ChessBoard board;		//A ChessBoard object to handle the logic of the Chess Game
	private final int computerTurn;			//int to store the computer
	private final Computer computer;
	private final int difficulty;
	private final ArrayList<Move> legal;	//A list to store the legal moves of a chess piece

	private boolean winner;
	private int click1;			//Stores the user first input

	public static long timeMoveGen = 0;
	public static long timePawnGen = 0;
	public static long timeKnightGen = 0;
	public static long timeSlidingGen = 0;
	public static long timeKingGen = 0;
	public static long timeValidMove = 0;
	public static long timeValidPart = 0;
	public static long timeMakeMove = 0;
	public static long timeUndoMove = 0;
	public static long timePawnAttack = 0;
	public static long timeKnightAttack = 0;
	public static long timeSlidingAttack = 0;
	public static long timeKingAttack = 0;
	public static long timeSoftAttack = 0;
	public static long timePieceUpdate = 0;
	public static long timeMisc = 0;
	public static long timeDebug = 0;
	public static long copyCount = 0;

	public ChessGame(int computerTurn, int difficulty){
		this.difficulty = difficulty;
		this.computerTurn = computerTurn;
		legal = new ArrayList<Move>(QUEEN);
		winner = false;
		click1 = -1;		//sets var as no clicks
		long prevTime;
		prevTime = System.currentTimeMillis();
		//rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8
		reset();
		prevTime = System.currentTimeMillis();
		Tests.runTests(); //5800, 8315, 3200
		System.out.println(System.currentTimeMillis() - prevTime);

		display();	//12582851

		reset();
		prevTime = System.currentTimeMillis();
		Tests.runTests(); //5800, 8315, 3200
		System.out.println(System.currentTimeMillis() - prevTime);

		display();	//12582851

		// Tests.test15.runTest();
		board = new ChessBoard();	//Creates a new ChessBoard object

		// BoardStorage store = board.copyData();
		// board.makeMove(new Move(55, 28, false));
		// board.makeMove(new Move(0, 56, false));
		// store = board.copyData();
		// ChessPiece piece = board.getPiece(56);
		// board.makeMove(new Move(28, 56, false));
		// board.undoMove(new Move (28, 56), piece, store);
		// board.makeMove(new Move(60, 52, false));
		// board.promote((byte) 1);
		// board.unPromote(2);
		// board.undoMove(new Move(11, 2, false), piece, store);
		// board.makeMove(new Move(11, 2, false));
		// board.promote((byte) 2);

		reset();
		computer = board.getComputer();
		prevTime = System.currentTimeMillis();
		System.out.println(computer.totalMoves(1));
		System.out.println(System.currentTimeMillis() - prevTime);
		prevTime = System.currentTimeMillis();
		System.out.println(computer.totalMoves(2));
		System.out.println(System.currentTimeMillis() - prevTime);
		prevTime = System.currentTimeMillis();
		System.out.println(computer.totalMoves(3));
		System.out.println(System.currentTimeMillis() - prevTime);
		prevTime = System.currentTimeMillis();
		System.out.println(computer.totalMoves(4));
		System.out.println(System.currentTimeMillis() - prevTime);
		prevTime = System.currentTimeMillis();
		System.out.println(computer.totalMoves(5));
		System.out.println(System.currentTimeMillis() - prevTime);
		prevTime = System.currentTimeMillis();
		System.out.println(computer.totalMoves(6));
		System.out.println(System.currentTimeMillis() - prevTime);
		display();	//129150836

		board.displayAttacks();
		// long prevTime = System.currentTimeMillis();
		// //Current (3, 33), (4, 145), (5, 1203), (6, 17052)
		// System.out.println(computer.totalMoves(6)); //Goal: (3, 0), (4, 11), (5, 259), (6, 6502)
		// System.out.println(System.currentTimeMillis() - prevTime);
		
		panel.setBorder(BorderFactory.createEmptyBorder(30,30,10,30));	//Creates a border
		panel.setLayout(new GridLayout(8,8));		//Creates an 8*8 grid for the squares
		GUI = new ChessSquare[64];					//ChessSquare array with a memory of 64
		//For loop to create 64 chess squares and add them to the panel
		for(int i=0; i< 64; i++) {
			GUI[i] = new ChessSquare(i, (Integer pos) -> get_click(pos));
			panel.add(GUI[i]);
		}
		frame.addKeyListener(new KeyListener((Byte type)-> promotion(type)));	//Adds a key listner for promotion
		frame.add(panel,BorderLayout.CENTER);		//Adds the panel to the frame
		frame.setSize(300,300);						//Sets the size of the new frame
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	//Allows the frame to commit seppuku
		frame.setTitle("Chess");			//I'm a creative mastermind
		frame.pack();						//Don't know what this does
		frame.setVisible(true);				//Makes the frame visible
		update_display();					//Updates the display
	}
	
	private void update_display() {
		/*ChessGame.upadate_display()-> None
		 * a function that updates every chess square
		 * with the correct piece and color*/
		// board.displayAttacks();
		String address;		//Stores the location of the correct image
		final String fen = board.getFenString();
		int count = 0;
		//For loop to cycle through all 64 squares
		for(int i = 0; i< fen.length(); i++) {
			if (count > 63) break;
			char letter = fen.charAt(i);
			//System.out.print(letter);
			int emptySpaces = Character.getNumericValue(letter);
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
					GUI[count].setIcon(resizeImage(new ImageIcon(address + ".png")));
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
			if(board.getPiece(count).color == WHITE) {
				address += "W";
			}
			address += letter +".png";
			GUI[count].setIcon(resizeImage(new ImageIcon(address)));	//Changes the image of the square
			count += 1;
		}
		if(click1 != -1) {
			for(int j = 0; j < legal.size(); j++) {
				final Move move = legal.get(j);
				System.out.print(move.finish +" : ");
				System.out.println(board.getPiece(move.finish).getType());
				//Checks if the square is a legal move
				if(board.getPiece(move.finish).isEmpty()) {
                    address = "Chess/Elements/";
					//address = "C:\\Users\\Mason\\Documents\\Java\\Elements\\";
					if(move.finish % 2 == (move.finish /8 ) %2){
						address += "W";
					}
					else {
						address += "G";
					}
					address += "D.png";
					GUI[move.finish].setIcon(resizeImage(new ImageIcon(address)));	//Changes the image of the square
				}
			}
			System.out.println();
		}
		if(board.getTurn() == computerTurn && winner == false) {
			computer_turn();
		}
	}
	
	private void computer_turn() {
//		Move move = board.computer_move(difficulty);
//		board.make_move(move,true);
//		if(board.is_promote()) {
//			move = board.computer_move(difficulty);
//			board.promote(board.promotingPawn, move.finish);
//		}
//		next_turn();
	}
	
	private void get_click(int pos) {
		/*ChessGame.get_click(int pos) - > None
		 * Function that runs when a ChessSquare is clicked
		 * processes the users click and selects a ChessPiece/Square*/
		//Checks if a promotion is happening
		if(board.is_promote() || board.getTurn() == computerTurn) {
			return;	//Nope
		}
		//Checks if the click is on the correct Chess pieces.
		if(board.getPiece(pos).color == board.getTurn() && !board.getPiece(pos).isEmpty()) {
			click1 = pos;		//Stores the users first click
			legal.clear();
			board.getPiece(click1).pieceMoves(legal); //Generates all the legal moves for a player
			//System.out.println(legal.size());
			update_display();	//Updates the display
			return;		//The return on your Bitcoin investment
		}
		//Checks if this is the second click
		if(click1 == -1) {
			return;		//Return to sender
		}	
		//for loop to cycle through every legal move
		for(int i = 0; i < legal.size(); i ++) {
			final Move move = legal.get(i);
			//Checks if the click is a legal move
			if(move.finish == pos) {
				//makes the move on the board
				board.makeMove(move);
				legal.clear();
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
	private void promotion(byte type) {
		/*ChessBoard.promotion(int type) -> Void
		 * function that checks if a promotion is happening
		 * then takes the users input and promotes a pawn*/
		//Checks if promotion is happening
		if(board.is_promote()) {
			board.promote(type);		//Promotes the pawn on the board
			next_turn();					//NEXT
		}
	}
	private void check_Win() {
		/*ChessBoard.check_Win() -> None
		 * function to check if a player has won
		 * the game*/
		final int win = board.isWinner();	//Checks if there is a winner

		//checks for no winner
		if(win == CONTINUE) {
			return;
		}
		final JFrame w2 = new JFrame();	//Cool frame
		final JPanel p2 = new JPanel();	//Cool panel
		JLabel l2 = new JLabel();	//Cool label
		final String[] colors = {"White","Black"};	//Colors
		//Checks for a win
		if(win == WIN) {
			l2 = new JLabel(colors[board.getTurn()] + " is the winner!");	//Displays winner
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

	private void next_turn() {
		/*ChessBoard.next_turn()-> None
		 * function to move on to the next
		 * players turn*/
		click1 =-1;	//Resets click variable
		check_Win();		//Checks for a win
		update_display();	//Updates the display
		System.out.println(board.getFenString());
		// board.displayAttacks();
		// System.out.println(computer.totalMoves(1));
		// count --;
	}

	public static ImageIcon resizeImage(ImageIcon imageIcon) {
		final Image image = imageIcon.getImage();
		final Image newImg = image.getScaledInstance(90, 90, java.awt.Image.SCALE_SMOOTH);
		return new ImageIcon(newImg);
	}

	private static void reset() {
		timeMoveGen = 0;
		timePawnGen = 0;
		timeKnightGen = 0;
		timeSlidingGen = 0;
		timeKingGen = 0;
		timeValidMove = 0;
		timeValidPart = 0;
		timeMakeMove = 0;
		timeUndoMove = 0;
		timePawnAttack = 0;
		timeKnightAttack = 0;
		timeSlidingAttack = 0;
		timeKingAttack = 0;
		timeSoftAttack = 0;
		timePieceUpdate = 0;
		timeMisc = 0;
		timeDebug = 0;
	}

	private static void display() {
		System.out.println("Move Generation: " + timeMoveGen);
		
		System.out.println("Pawn Move: " + timePawnGen);
		System.out.println("Knight Move: " + timeKnightGen);
		System.out.println("Sliding Move: " + timeSlidingGen);
		System.out.println("King Move: " + timeKingGen);
		System.out.println("Valid Move: " + timeValidMove);
		System.out.println("Valid Part: " + timeValidPart);
		System.out.println("Make Move: " + timeMakeMove);
		System.out.println("Undo Move: " + timeUndoMove);
		System.out.println("Pawn Attack: " + timePawnAttack);
		System.out.println("Knight Attack: " + timeKnightAttack);
		System.out.println("Sliding Attack: " + timeSlidingAttack);
		System.out.println("King Attack: " + timeKingAttack);
		System.out.println("Soft Attack: " + timeSoftAttack);
		System.out.println("Piece Update: " + timePieceUpdate);
		System.out.println("Misc: " + timeMisc);
		System.out.println("Debug: " + timeDebug);
		System.out.println("Moves Copied: " + copyCount);
	}
}
