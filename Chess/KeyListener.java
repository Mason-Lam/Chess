package Chess;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class KeyListener extends KeyAdapter {

	private final Consumer<Byte> promotion;

	public KeyListener(Consumer<Byte> promotion) {
		this.promotion = promotion;
	}

	/*Extends Key Adapter and overrides keyPressed function*/
	@Override
	public void keyPressed(KeyEvent event) {
		/*Activates every time a key is pressed*/
		char ch = event.getKeyChar();	//Gets what key was pressed
		byte type;		//Variable to store what the user wants to promote 
		//Checks if the key pressed is a k
//		if(ch == 'c') {
//			ChessGame.computer = 0;
//			ChessGame.update_display();
//		}
		if(ch == 'k') {
			type = Constants.KNIGHT;
		}
		//Checks if the key pressed is a q
		else if(ch == 'q') {
			type = Constants.QUEEN;
		}
		//Checks if the key pressed is a r
		else if(ch == 'r') {
			type = Constants.ROOK;
		}
		//Checks if the key pressed is a b
		else if(ch == 'b') {
			type = Constants.BISHOP;
		}
		//Invalid Input
		else {
			return;
		}
		promotion.accept(type); //Promotes the piece
	}
}
