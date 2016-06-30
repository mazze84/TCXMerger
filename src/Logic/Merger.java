package Logic;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Merger {
	public File connect = null;
	public File runtastic = null;

	public Document connectDoc;
	public Document runtasticDoc;

	DocumentBuilderFactory factory;

	public enum GarminXML {

		ACTIVITIES("Activities"), ACTIVITY("Activity"), LAP("Lap"), TRACK("Track"), TRACKPOINT("Trackpoint"), TIME(
				"Time"), MAX_HEARTRATE("MaximumHeartRateBpm"), AVG_HEARTRATE(
						"AverageHeartRateBpm"), HEARTRATE("HeartRateBpm"), CALORIES("Calories");

		private String xmlElement;

		private GarminXML(String element) {
			this.xmlElement = element;
		}

		public String getElementName() {
			return xmlElement;
		}

	};

	public Merger(File connect, File runtastic) throws ParserConfigurationException, SAXException, IOException {
		this.connect = connect;
		this.runtastic = runtastic;

		this.factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		this.connectDoc = builder.parse(this.connect);
		// this.runtasticDoc = builder.parse(this.runtastic);
	}

	public void merge() {

	}

	public List<Node> getTrackPoints(Document doc) {
		List<Node> trackPoints = new LinkedList<Node>();

		NodeList activities = doc.getDocumentElement().getElementsByTagName(GarminXML.ACTIVITIES.getElementName());

		if (activities.getLength() == 1) {
			Node activityNode = activities.item(0).getFirstChild();
			if (activityNode.getNodeName().equals(GarminXML.ACTIVITY.getElementName())) {
				Node node = activityNode.getNextSibling();
				for (; node != null; activityNode.getNextSibling()) {
					if (node.getNodeName().equals(GarminXML.LAP.getElementName())) {

					}
				}
			}
		}
		return trackPoints;
	}

	private Calendar extractTime(String time) {
		// <Time>2016-06-25T10:57:01.000Z</Time>
		Calendar cal = Calendar.getInstance();

		int year = Integer.valueOf(time.substring(0, 4));
		int month = Integer.valueOf(time.substring(5, 7));
		int day = Integer.valueOf(time.substring(8, 10));

		int hour = Integer.valueOf(time.substring(11, 13));
		int min = Integer.valueOf(time.substring(14, 16));
		int sec = Integer.valueOf(time.substring(17, 19));

		cal.set(year, month, day, hour, min, sec);

		return cal;
	}

	public int checkTime(String connectTime, String runtasticTime) {
		Calendar connectCal = extractTime(connectTime);
		Calendar runtasticCal = extractTime(runtasticTime);
		return connectCal.compareTo(runtasticCal);
	}

	public Node insertIntoNode(Node parent, List<Node> inserts) {
		for (Node node : inserts) {
			parent.appendChild(node);
		}

		return parent;
	}

	public void buildXMLTree() {

	}

	private void writeFile() {
		// TODO: write new tcx file
	}

	public static void main(String args[]) {

		File connect = new File("activity_1229268573.tcx");
		File runtastic = new File("");
		try {
			Merger merger = new Merger(connect, runtastic);
			int check = merger.checkTime("2016-06-25T10:57:01.000Z", "2016-06-25T10:59:04.000Z");

			System.out.println(check);

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
