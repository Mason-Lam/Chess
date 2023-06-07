package Chess;
import javax.swing.*;
public class ChessSquare extends JLabel{
	/* A JLabel that display ChessSquares*/
	private static final long serialVersionUID = 1L;	//Don't know why tf this here but Eclipse said to have
	int position;	//Variable to store where the ChessSquare is located.
	public ChessSquare(int pos) {
		/*ChessSquare square = new ChessSquare(int pos)-> ChessSquare
		 * creates a ChessSquare with a specified positions and a mouse
		 * listener*/
		//super(new ImageIcon("C:\\Users\\Mason\\Documents\\Java\\W.png"));
        super(ChessGame.resizeImage(new ImageIcon("Chess/Elements/W.png")));
		position = pos;		//sets the positions
		MouseListener listener = new MouseListener();	//creates a mouse listener
		addMouseListener(listener);	//Listens for clicks on the square
	}
	public void clicked() {
		/*ChessSquare.clicked()-> None
		 * a function that runs when the square is clicked
		 * and alerts the ChessGame*/
		ChessGame.get_click(position);	//Calls the get_click function and inputs the squares position
	}
}
