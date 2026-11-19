import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import javax.imageio.ImageIO;

import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;

import java.awt.*;

public class Driver {
	
	// Declare class data
    // Moves the raccoon forward and leaves a line behind it.
    private static class TripAnimator {
        private final JMapViewer mapViewer;
        private final Image raccoonImage;
        private final Image stopImage;
        private final JLabel statusLabel;
        private Timer animationTimer;
        private ArrayList<TripPoint> points;
        private ArrayList<TripPoint> stopLocations;
        private boolean includeStops;
        private int currentPointIndex;
        private IconMarker currentMarker;

        private TripAnimator(JMapViewer mapViewer, Image raccoonImage, Image stopImage, JLabel statusLabel) {
            this.mapViewer = mapViewer;
            this.raccoonImage = raccoonImage;
            this.stopImage = stopImage;
            this.statusLabel = statusLabel;
        }

        private void start(ArrayList<TripPoint> selectedPoints, ArrayList<TripPoint> detectedStops,
                boolean includeStops, int durationSeconds) {
            if (animationTimer != null) {
                animationTimer.stop();
            }

            mapViewer.removeAllMapMarkers();
            mapViewer.removeAllMapPolygons();
            points = new ArrayList<>(selectedPoints);
            stopLocations = detectedStops;
            this.includeStops = includeStops;

            if (points.isEmpty()) {
                statusLabel.setText("There are no trip points to animate.");
                return;
            }

            currentPointIndex = 1;
            currentMarker = createMarker(points.get(0));
            mapViewer.addMapMarker(currentMarker);

            if (points.size() == 1) {
                statusLabel.setText("Animation complete.");
                return;
            }

            int stepCount = points.size() - 1;
            int delayMilliseconds = Math.max(1, durationSeconds * 1000 / stepCount);

            // A Swing timer updates the map a little at a time without freezing the window.
            animationTimer = new Timer(delayMilliseconds, event -> showNextPoint());
            statusLabel.setText("Animating point 1 of " + points.size() + ".");
            animationTimer.start();
        }

        private void showNextPoint() {
            TripPoint previousPoint = points.get(currentPointIndex - 1);
            TripPoint nextPoint = points.get(currentPointIndex);

            Coordinate nextCoordinate = new Coordinate(nextPoint.getLat(), nextPoint.getLon());
            // JMapViewer skips polygons with fewer than three coordinates, so repeat the endpoint.
            MapPolygonImpl trackSegment = new MapPolygonImpl(
                    new Coordinate(previousPoint.getLat(), previousPoint.getLon()),
                    nextCoordinate,
                    nextCoordinate);
            trackSegment.setColor(Color.RED);
            trackSegment.setBackColor(null);
            trackSegment.setStroke(new BasicStroke(3));
            mapViewer.addMapPolygon(trackSegment);

            mapViewer.removeMapMarker(currentMarker);
            currentMarker = createMarker(nextPoint);
            mapViewer.addMapMarker(currentMarker);

            if (includeStops && stopLocations.contains(nextPoint)) {
                mapViewer.addMapMarker(new IconMarker(
                        new Coordinate(nextPoint.getLat(), nextPoint.getLon()), stopImage));
            }

            currentPointIndex++;
            if (currentPointIndex == points.size()) {
                animationTimer.stop();
                statusLabel.setText("Animation complete.");
            } else {
                statusLabel.setText("Animating point " + currentPointIndex + " of " + points.size() + ".");
            }
        }

        private IconMarker createMarker(TripPoint point) {
            return new IconMarker(new Coordinate(point.getLat(), point.getLon()), raccoonImage);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {

        // Read file and call stop detection
        TripPoint.readFile("triplog.csv");
        int stoppedPointCount = TripPoint.h1StopDetection();
        ArrayList<TripPoint> allPoints = TripPoint.getTrip();
        ArrayList<TripPoint> movingPoints = TripPoint.getMovingTrip();
        ArrayList<TripPoint> stopLocations = getStopLocations(allPoints, movingPoints);
        Image raccoonImage = ImageIO.read(new File("raccoon.png"));
        if (raccoonImage == null) {
            throw new IOException("Could not read raccoon.png.");
        }
        Image stopImage = ImageIO.read(new File("stop.png"));
        if (stopImage == null) {
            throw new IOException("Could not read stop.png.");
        }

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

        JLabel statusLabel = new JLabel(
                "Loaded " + allPoints.size() + " points; H1 detected " + stoppedPointCount + " stopped points.");
    	
        // Add all to top panel
        topPanel.add(playButton);

        topPanel.add(stopCheckBox);
        topPanel.add(timeComboBox);
        topPanel.setLayout(new FlowLayout());
        topPanel.setPreferredSize(new Dimension(0, 50));
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(statusLabel, BorderLayout.SOUTH);

        // Set up mapViewer
        JMapViewer mapViewer = new JMapViewer();
        mapViewer.setTileLoader(new OsmTileLoader(mapViewer, Collections.singletonMap(
                "User-Agent", "JavaTripMap/1.0 (https://github.com/sanjana3/Project4_starter)")));
        frame.add(mapViewer, BorderLayout.CENTER);

        TripAnimator animator = new TripAnimator(mapViewer, raccoonImage, stopImage, statusLabel);
        
        // Add listeners for GUI components
        stopCheckBox.addActionListener(event ->
                updateSelectionStatus(statusLabel, timeComboBox, stopCheckBox, allPoints, movingPoints));
        timeComboBox.addActionListener(event ->
                updateSelectionStatus(statusLabel, timeComboBox, stopCheckBox, allPoints, movingPoints));
        playButton.addActionListener(event -> {
            if (timeComboBox.getSelectedIndex() == 0) {
                statusLabel.setText("Select an animation time before pressing Play.");
                return;
            }

            int durationSeconds = Integer.parseInt((String) timeComboBox.getSelectedItem());
            ArrayList<TripPoint> selectedPoints = stopCheckBox.isSelected() ? allPoints : movingPoints;
            animator.start(selectedPoints, stopLocations, stopCheckBox.isSelected(), durationSeconds);
        });

        // Set the map center and zoom level
        
        frame.setVisible(true);
        centerMapOnTrip(mapViewer, allPoints);
    }
    
    // Animate the trip based on selections from the GUI components

    private static void updateSelectionStatus(JLabel statusLabel, JComboBox<String> timeComboBox,
            JCheckBox stopCheckBox, ArrayList<TripPoint> allPoints, ArrayList<TripPoint> movingPoints) {
        if (timeComboBox.getSelectedIndex() == 0) {
            statusLabel.setText("Select an animation time.");
            return;
        }

        int pointCount = stopCheckBox.isSelected() ? allPoints.size() : movingPoints.size();
        String stopSetting = stopCheckBox.isSelected() ? "with stops" : "without stops";
        statusLabel.setText("Selected " + pointCount + " points for " + timeComboBox.getSelectedItem()
                + " seconds " + stopSetting + ".");
    }

    private static ArrayList<TripPoint> getStopLocations(ArrayList<TripPoint> allPoints,
            ArrayList<TripPoint> movingPoints) {
        ArrayList<TripPoint> stopLocations = new ArrayList<>();
        boolean previousPointWasStopped = false;

        for (TripPoint point : allPoints) {
            boolean pointWasStopped = !movingPoints.contains(point);
            if (pointWasStopped && !previousPointWasStopped) {
                stopLocations.add(point);
            }
            previousPointWasStopped = pointWasStopped;
        }

        return stopLocations;
    }

    private static void centerMapOnTrip(JMapViewer mapViewer, ArrayList<TripPoint> points) {
        double minimumLatitude = Double.MAX_VALUE;
        double maximumLatitude = -Double.MAX_VALUE;
        double minimumLongitude = Double.MAX_VALUE;
        double maximumLongitude = -Double.MAX_VALUE;

        for (TripPoint point : points) {
            minimumLatitude = Math.min(minimumLatitude, point.getLat());
            maximumLatitude = Math.max(maximumLatitude, point.getLat());
            minimumLongitude = Math.min(minimumLongitude, point.getLon());
            maximumLongitude = Math.max(maximumLongitude, point.getLon());
        }

        // Find a zoom level that fits the full trip inside the visible map.
        double longitudeSpan = Math.max((maximumLongitude - minimumLongitude) / 360.0, 0.000001);
        double northWorldY = latitudeToWorldY(maximumLatitude);
        double southWorldY = latitudeToWorldY(minimumLatitude);
        double latitudeSpan = Math.max(southWorldY - northWorldY, 0.000001);
        double availableWidth = Math.max(1, mapViewer.getWidth() - 80);
        double availableHeight = Math.max(1, mapViewer.getHeight() - 80);
        double zoomForWidth = Math.log(availableWidth / (256.0 * longitudeSpan)) / Math.log(2.0);
        double zoomForHeight = Math.log(availableHeight / (256.0 * latitudeSpan)) / Math.log(2.0);
        int zoom = (int) Math.floor(Math.min(zoomForWidth, zoomForHeight));
        zoom = Math.max(JMapViewer.MIN_ZOOM, Math.min(JMapViewer.MAX_ZOOM, zoom));

        double centerWorldY = (northWorldY + southWorldY) / 2.0;
        double centerLatitude = Math.toDegrees(
                Math.atan(Math.sinh(Math.PI * (1.0 - 2.0 * centerWorldY))));
        double centerLongitude = (minimumLongitude + maximumLongitude) / 2.0;
        mapViewer.setDisplayPosition(new Coordinate(centerLatitude, centerLongitude), zoom);
    }

    private static double latitudeToWorldY(double latitude) {
        // Map tiles use a curved projection, so convert latitude before measuring map height.
        double limitedLatitude = Math.max(-85.05112878, Math.min(85.05112878, latitude));
        double sinLatitude = Math.sin(Math.toRadians(limitedLatitude));
        return 0.5 - Math.log((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * Math.PI);
    }
    
}