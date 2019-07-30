package mapView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.LatLongBounds;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import mapView.Rectangle;
import com.lynden.gmapsfx.shapes.RectangleOptions;
import javafx.application.Application;
import netscape.javascript.JSObject;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * Implements Google Maps via GMapsFX 
 * Credits to rterp @ http://rterp.github.io/GMapsFX/
 * This class is the primary driver class that displays our google map with the well locations.
 * @author huan482
 *
 */
public class GMapView extends Application implements MapComponentInitializedListener {
	private static final int INIT_ZOOM = 13; 
	
	private GoogleMapView myMapView;
	
	private GoogleMap myMap;
	
	private MapOptions myMapOpts;
	
	private Stage myStage;
	
	private double myMinBoundsX;
	
	private double myMinBoundsY;
	
	private double myMaxBoundsX;
	
	private double myMaxBoundsY;
	
//	private List<Float> myXGrid;
//	
//	private List<Float> myYGrid;
	
	private List<Float> mySouthWestXLongitudes;
	
	private List<Float> mySouthWestYLatitudes;
	
	private List<Float> myNorthEastXLongitudes;
	
	private List<Float> myNorthEastYLatitudes;
	
	private List<IJ> myBoxes;
	
	private Map<LatLongBounds, IJ> myBoxMapping;
	
	/**
	 * JavaFX start method.
	 */
	@Override
	public void start(final Stage theStage) throws Exception {
//		myXGrid = new ArrayList<Float>();
//		myYGrid = new ArrayList<Float>();
		myStage = theStage;
		//Google Maps API Key, 2nd parameter
		myMapView = new GoogleMapView("en", "AIzaSyCqMjOt2Q17PnE9-9843sutOpihbglC_6k");
		myMapView.addMapInializedListener(this);

		//My Scene
		Scene scene = new Scene(myMapView);
		
		//The stage
		myStage.setTitle("Well Locations");
		myStage.setScene(scene);
		myStage.show();
		
	}
	
	/**
	 * Initializes the map by specifying the launch center of our map and also draws the bounds and rectangles.
	 */
	@Override
	public void mapInitialized() {
		initVariables();
		myMapOpts = new MapOptions(); 
		//Opens up console for debugging purposes
//		myMapView.getWebview().getEngine().executeScript("if (!document.getElementById"
//				+ "('FirebugLite')){E = document['createElement' + 'NS']"
//				+ " && document.documentElement.namespaceURI;E = E ?"
//				+ " document['createElement' + 'NS'](E, 'script') "
//				+ ": document['createElement']('script');E['setAttribute']"
//				+ "('id', 'FirebugLite');E['setAttribute']"
//				+ "('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');"
//				+ "E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']"
//				+ "('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E"
//				+ " = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");	
		
		myMapOpts.center(new LatLong((myMaxBoundsY + myMinBoundsY) / 2, (myMaxBoundsX + myMinBoundsX) / 2))
				 .mapType(MapTypeIdEnum.ROADMAP)
				 .overviewMapControl(false)
				 .panControl(false)
				 .rotateControl(false)
				 .scaleControl(false) 
				 .streetViewControl(false)
				 .zoomControl(false)
				 .zoom(INIT_ZOOM);
		
		myMap = myMapView.createMap(myMapOpts);
		drawBoundPolyLines();
		createRectangles();
	}
	
	/** 
	 * Grabs the variables from our GMapInitVar class.
	 */
	private void initVariables() {
//		myXGrid = GMapInitVar.getXGrid();
//		myYGrid = GMapInitVar.getYGrid();
		myBoxes = GMapInitVar.getMyBoxes();
		myMinBoundsX = GMapInitVar.getMinBoundX();
		myMinBoundsY = GMapInitVar.getMinBoundY();
		myMaxBoundsX = GMapInitVar.getMaxBoundX();
		myMaxBoundsY= GMapInitVar.getMaxBoundY();
		myBoxMapping = new HashMap<LatLongBounds, IJ>();
		myNorthEastXLongitudes = GMapInitVar.getMyNorthEastXLongitude();
		myNorthEastYLatitudes = GMapInitVar.getMyNorthEastYLatitude();
		mySouthWestXLongitudes = GMapInitVar.getMySouthWestXLongitude();
		mySouthWestYLatitudes = GMapInitVar.getMySouthWestYLatitude();
//		System.out.println("Bounds: " + myMinBoundsX + " " + myMinBoundsY + " " + myMaxBoundsX + " " + myMaxBoundsY);
	}
	
	/**
	 * Draws the bounds of our entire space via poly lines.
	 */
	private void drawBoundPolyLines() {
		//To draw a rectangle find the NE and SW points of your rectangle (Lat,Long) then draw it.
		LatLong topRight = new LatLong(myMaxBoundsY, myMaxBoundsX);
		LatLong bottomleft = new LatLong(myMinBoundsY, myMinBoundsX);
		//LatLongBounds(SW,NE)
		LatLongBounds llb = new LatLongBounds(bottomleft, topRight);
		RectangleOptions rOpts = new RectangleOptions()
				.bounds(llb)
				.strokeColor("red")
				.strokeWeight(2)
				.fillColor(null)
				.fillOpacity(0);
		Rectangle rt = new Rectangle(rOpts);
		myMap.addMapShape(rt);
//		System.out.println(rOpts.getJSObject().getMember("strokeColor"));
//		for (int i = 0; i < myXGrid.size(); i += 2) {
//			LatLong[] ary = new LatLong[] {new LatLong(myMinBoundsY, myXGrid.get(i)),
//					new LatLong(myMaxBoundsY, myXGrid.get(i))};
//			MVCArray mvc = new MVCArray(ary);
//			PolylineOptions polyOpts = new PolylineOptions()
//					.path(mvc)
//					.strokeColor("red")
//					.strokeWeight(2);
//			Polyline poly = new Polyline(polyOpts);
//			myMap.addMapShape(poly);
//		} 
//		
//		for (int i = 0; i < myYGrid.size(); i+= 2) {
//			LatLong[] ary = new LatLong[] {new LatLong(myYGrid.get(i), myMinBoundsX),
//					new LatLong(myYGrid.get(i), myMaxBoundsX)};
//			MVCArray mvc = new MVCArray(ary);
//			PolylineOptions polyOpts = new PolylineOptions()
//					.path(mvc)
//					.strokeColor("red")
//					.strokeWeight(2);
//			Polyline poly = new Polyline(polyOpts);
//			myMap.addMapShape(poly);
//		}
	}
	
	/**
	 * Creates the rectangles that represent out well locations and also adds the action listeners
	 * required.
	 */
	private void createRectangles() {
		for (int i = 0; i < myNorthEastXLongitudes.size(); i++) {
			LatLong SouthWest = new LatLong(mySouthWestYLatitudes.get(i), mySouthWestXLongitudes.get(i));
			LatLong NorthEast = new LatLong(myNorthEastYLatitudes.get(i), myNorthEastXLongitudes.get(i));
			LatLongBounds rectangleBounds = new LatLongBounds(SouthWest,NorthEast);
			RectangleOptions rectOpts = new RectangleOptions()
					.bounds(rectangleBounds)
					.strokeColor("black")
					.strokeWeight(2)
					.clickable(true)
					.fillColor("black");
			Rectangle rect = new Rectangle(rectOpts);
			myMap.addMapShape(rect);
			myBoxMapping.put(rectangleBounds, myBoxes.get(i));
			myMap.addUIEventHandler(rect, UIEventType.click, (JSObject obj) -> {
				if (rect.getJSObject().getMember("fillColor").equals("black")) {
					myMap.removeMapShape(rect);
					rectOpts.bounds(rect.getBounds()).fillColor("white");
					rect.setRectangleOptions(rectOpts);
					myMap.addMapShape(rect);
					selectingWells(rect.getBounds(), false);
				} else {			
					myMap.removeMapShape(rect);
					rectOpts.bounds(rect.getBounds()).fillColor("black");
					rect.setRectangleOptions(rectOpts);
					myMap.addMapShape(rect);
					selectingWells(rect.getBounds(), true);
				}						
			});
		}
	}
	
	/**
	 * Sets the check boxes on the exclude locations page to selected or de-selected 
	 * based on whether the rectangle inside the google maps window was selected
	 * or de-selected.
	 * @param theLLB - The LatLongBounds from the rectangle that was clicked.
	 * @param allowWell - Boolean value for whether we should select or de-select the well.
	 */
	private void selectingWells(final LatLongBounds theLLB, final boolean allowWell) {
		for (LatLongBounds key: myBoxMapping.keySet()) {
			if (checkEqualLLB(key, theLLB)) {
				myBoxMapping.get(key).prohibited = allowWell;
			}
		
		}
	}
	/**
	 * LatLongBound equality and LatLong equality doesn't output correct boolean value.
	 * Had to decompose the LatLongBounds to it's each individual Latitude and Longitude and then
	 * check those.
	 * @param keySet - The key from the KeySet
	 * @param theLLB - The LatLongBounds from the rectangle that was clicked.
	 * @return - Whether they are the same LatLongBounds.
	 */
	private boolean checkEqualLLB(final LatLongBounds keySet, final LatLongBounds theLLB) {
		return keySet.getNorthEast().getLatitude() == theLLB.getNorthEast().getLatitude()
				&& keySet.getNorthEast().getLongitude() == theLLB.getNorthEast().getLongitude()
				&& keySet.getSouthWest().getLatitude() == theLLB.getSouthWest().getLatitude()
				&& keySet.getSouthWest().getLongitude() == theLLB.getSouthWest().getLongitude();
	}
}