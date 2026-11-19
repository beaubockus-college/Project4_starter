import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.*;
import java.awt.*;

public class Driver {
	
	// Declare class data

    public static void main(String[] args) throws FileNotFoundException, IOException {

    	// Read file and call stop detection
    	
    	
    	// Set up frame, include your name in the title
    	JFrame frame = new JFrame("Trip Animation - Beau Bockus");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());
        
        // Set up Panel for input selections
        JPanel topPanel = new JPanel();
        frame.add(topPanel);
    	
        // Play Button
        JButton playButton = new JButton("Play");
    	
        // CheckBox to enable/disable stops
        JCheckBox stopCheckBox = new JCheckBox("Include Stops");
        
        // ComboBox to pick animation time
        JComboBox<String> timeComboBox = new JComboBox<>(new String[]{"Animation Time", "15", "30", "60", "90"});
    	
        // Add all to top panel
        topPanel.add(playButton);

        topPanel.add(stopCheckBox);
        topPanel.add(timeComboBox);
        topPanel.setLayout(new FlowLayout());
        topPanel.setPreferredSize(new Dimension(0, 50));
        frame.add(topPanel, BorderLayout.NORTH);
        // Set up mapViewer
        
        
        // Add listeners for GUI components
        

        // Set the map center and zoom level
        
        frame.setVisible(true);
    }
    
    // Animate the trip based on selections from the GUI components
    
}