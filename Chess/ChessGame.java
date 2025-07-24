package Chess;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;

import javax.swing.*;

import Chess.Constants.PieceConstants.PieceColor;
import Chess.Constants.PieceConstants.PieceType;

import static Chess.Constants.EvaluateConstants.*;

public class ChessGame {
	/*A Chess Game class*/
	private final JFrame frame = new JFrame();	//Frame to display the ChessGame
	private final JPanel panel = new JPanel();	//Panel to display the Chess Squares
	private final ChessSquare[] GUI;	//An array to store the Chess Squares
	private final ChessBoard board;		//A ChessBoard object to handle the logic of the Chess Game
	private final PieceColor computerTurn;			//int to store the computer
	private final Computer computer;
	private final int difficulty;
	private final PieceColor perspective;
	private final ArrayList<Move> legal;	//A list to store the legal moves of a chess piece

	private boolean winner;
	private int click1;			//Stores the user first input

	public ChessGame(PieceColor computerTurn, int difficulty) {
		this(computerTurn, difficulty, PieceColor.WHITE);
	}

	public ChessGame(PieceColor computerTurn, int difficulty, PieceColor perspective){
		this.computerTurn = computerTurn;
		this.difficulty = difficulty;
		this.perspective = perspective;

		legal = new ArrayList<Move>();
		winner = false;
		click1 = -1;		//sets var as no clicks

		board = new ChessBoard(Tests.test15.fen);	//Creates a new ChessBoard object
		this.computer = board.getComputer();

		// Tests.timeCheckBoard(board, 6);
		// //Current (3, 0), (4, 16), (5, 316), (6, 8018)
		// System.out.println(computer.totalMoves(6)); //Goal: (3, 0), (4, 11), (5, 259), (6, 6502)
		
		panel.setBorder(BorderFactory.createEmptyBorder(30,30,10,30));	//Creates a border
		panel.setLayout(new GridLayout(8,8));		//Creates an 8*8 grid for the squares
		GUI = new ChessSquare[64];					//ChessSquare array with a memory of 64
		//For loop to create 64 chess squares and add them to the panel
		for(int i=0; i< 64; i++) {
			GUI[i] = new ChessSquare(i, (Integer pos) -> get_click(pos));
			panel.add(GUI[i]);
		}
		frame.addKeyListener(new KeyListener((PieceType type)-> promotion(type)));	//Adds a key listner for promotion
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
					GUI[perspective == PieceColor.BLACK ? 63 - count : count].setIcon(resizeImage(new ImageIcon(address + ".png")));
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
			if(board.getPiece(count).color == PieceColor.WHITE) {
				address += "W";
			}
			address += letter +".png";
			GUI[perspective == PieceColor.BLACK ? 63 - count : count].setIcon(resizeImage(new ImageIcon(address)));	//Changes the image of the square
			count += 1;
		}
		if(click1 != -1) {
			for(int j = 0; j < legal.size(); j++) {
				final Move move = legal.get(j);
				System.out.print(move.getFinish() +" : ");
				System.out.println(board.getPiece(move.getFinish()).getType() + " : " + move.isSpecial());
				//Checks if the square is a legal move
				if(board.getPiece(move.getFinish()).isEmpty()) {
                    address = "Chess/Elements/";
					//address = "C:\\Users\\Mason\\Documents\\Java\\Elements\\";
					if(move.getFinish() % 2 == (move.getFinish() /8 ) %2){
						address += "W";
					}
					else {
						address += "G";
					}
					address += "D.png";
					GUI[perspective == PieceColor.BLACK ? 63 - move.getFinish() : move.getFinish()].setIcon(resizeImage(new ImageIcon(address)));	//Changes the image of the square
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
//			board.promote(board.promotingPawn, move.getFinish());
//		}
//		next_turn();
	}
	
	private void get_click(int pos) {
		/*ChessGame.get_click(int pos) - > None
		 * Function that runs when a ChessSquare is clicked
		 * processes the users click and selects a ChessPiece/Square*/
		pos = perspective == PieceColor.BLACK ? 63 - pos : pos;
		//Checks if a promotion is happening
		if(board.is_promote() || board.getTurn() == computerTurn) {
			return;	//Nope
		}
		//Checks if the click is on the correct Chess pieces.
		if(board.getPiece(pos).color == board.getTurn() && !board.getPiece(pos).isEmpty()) {
			click1 = pos;		//Stores the users first click
			legal.clear();
			board.getBitboard().generatePieceMoves(legal, pos, false);
			// board.getPiece(click1).pieceMoves(legal); //Generates all the legal moves for a player
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
			if(move.getFinish() == pos) {
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
	private void promotion(PieceType type) {
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
			l2 = new JLabel(colors[board.getTurn().arrayIndex] + " is the winner!");	//Displays winner
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
		BitboardHelper.displayBitboard(board.getBitboard().generateKingBitboard(PieceColor.WHITE, 60, false));
		// BitboardHelper.displayBitboard(board.getBitboard().ATTACKS[0]);
		// BitboardHelper.displayBitboard(board.getBitboard().ATTACKS[1]);
		// board.displayAttacks();
		// System.out.println(computer.totalMoves(1));
		// count --;
	}

	public static ImageIcon resizeImage(ImageIcon imageIcon) {
		final Image image = imageIcon.getImage();
		final Image newImg = image.getScaledInstance(90, 90, java.awt.Image.SCALE_SMOOTH);
		return new ImageIcon(newImg);
	}

}
