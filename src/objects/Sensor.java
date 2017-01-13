package objects;

import java.util.Map;

import utilities.Point3f;
import utilities.Point3i;

/**
 * Basic monitoring technology class
 * @author port091
 * @author rodr144
 */

public class Sensor {
	
	//Static functions for accessing the aliases
	
	static public Map<String, String> sensorAliases;
	
	// About the sensors location
	protected Point3i node;
	protected Point3f point;
	protected int nodeNumber;
	
	// What type of sensor
	protected String type;
		
    public Sensor(float x, float y, float z, String type, NodeStructure domain) {
    	
    	point = new Point3f(x,y,z);
    	node = domain.getIJKFromXYZ(point);
    	nodeNumber = domain.getNodeNumber(node);
    	
    	this.type = type;
    }
    
    public Sensor(int i, int j, int k, String type, NodeStructure domain) {
    	
    	node =  new Point3i(i,j,k);
    	nodeNumber = domain.getNodeNumber(node);
    	point = domain.getXYZEdgeFromIJK(node);
    	
    	this.type = type;
    }
    
    public Sensor(int nodeNumber, String type, NodeStructure domain) {
    	
    	this.nodeNumber = nodeNumber;
    	node = domain.getIJKFromNodeNumber(nodeNumber);
    	point = domain.getXYZEdgeFromIJK(node);
    	
    	this.type = type;
    }
    
    public Sensor(Sensor toCopy) {
    	
    	this.nodeNumber = toCopy.nodeNumber;
    	this.node = new Point3i(toCopy.node);
    	if(this.point != null)
    		this.point = new Point3f(toCopy.point);
    	
    	this.type = toCopy.type;    	
    }
	
	public Integer getNodeNumber() {
		return nodeNumber;
	}
	
	public String getSensorType() {
		return type;
	}
	
	public Point3i getIJK() {
		return node;
	}
	
	public Point3f getPoint() {
		return point;
	}	
	
    @Override
    public int hashCode() {
    	return type != null ? type.hashCode() * 37 + nodeNumber : nodeNumber;
    } 

}
