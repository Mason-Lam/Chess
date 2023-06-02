package Chess;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class MouseListener extends MouseAdapter {
	/*Extends MouseAdapter and overrides mouseClicked function*/
    @Override
    public void mouseClicked(MouseEvent e) {
    	/*Not going to  lie, have no idea how this works but
    	 the function essentially alerts the program
    	 a certain square has been clicked */
         ChessSquare square = (ChessSquare) e.getSource();	//Gets the source square
         square.clicked();					//Calls the ChessSquare clicked function
    }
}