package Chess;
import java.util.function.Consumer;

import javax.swing.*;
public class ChessSquare extends JLabel{
	/* A JLabel that display ChessSquares*/
	private final int pos;	//Variable to store where the ChessSquare is located.
	private final Consumer<Integer> getClick;

	public ChessSquare(int pos, Consumer<Integer> getClick) {
		/*ChessSquare square = new ChessSquare(int pos)-> ChessSquare
		 * creates a ChessSquare with a specified positions and a mouse
		 * listener*/
		//super(new ImageIcon("C:\\Users\\Mason\\Documents\\Java\\W.png"));
        super(ChessGame.resizeImage(new ImageIcon("Chess/Elements/W.png")));
		this.pos = pos;		//sets the positions
		this.getClick = getClick;
		final MouseListener listener = new MouseListener();	//creates a mouse listener
		addMouseListener(listener);	//Listens for clicks on the square
	}
	public void clicked() {
		/*ChessSquare.clicked()-> None
		 * a function that runs when the square is clicked
		 * and alerts the ChessGame*/
		getClick.accept(pos); //Calls the get_click function and inputs the squares position
	}
}
