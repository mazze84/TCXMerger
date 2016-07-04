package Logic;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Merger {
	private File connect = null;
	private File runtastic = null;

	private Document connectDoc;
	private Document runtasticDoc;

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

	public NodeList merge(NodeList connect, NodeList runtastic) {
		int connectLength = connect.getLength();
		int runtasticLength = runtastic.getLength();

		int length = Math.max(connectLength, runtasticLength);

		for (int index = 0; index < length; index++) {
			Node runtasticNode = null;
			if (runtasticLength < index) {
				runtasticNode = runtastic.item(index);
			}
			Node connectNode = null;
			if (connectLength < index) {
				connectNode = connect.item(index);
			}

			Node time = connect.item(index).getFirstChild();

		}

		// TODO: add the merged NodeList
		return connect;

	}

	public NodeList getTrackPoints(Document doc) {

		Node activities = doc.getDocumentElement().getChildNodes().item(0).getNextSibling();

		// if (activities.getLength() == 1) {
		Node activityNode = activities.getChildNodes().item(0).getNextSibling();

		Node lap = activityNode.getChildNodes().item(2).getNextSibling();

		NodeList lapChilds = lap.getChildNodes();

		for (int index = 0; index < lapChilds.getLength(); index++) {
			Node node = lapChilds.item(index);
			if (node.getNodeName().equals(GarminXML.TRACK.getElementName())) {
				return node.getChildNodes();
			}
		}

		return null;
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
		File runtastic = new File("runtastic_20160625_1142_Radfahren.tcx");
		try {
			Merger merger = new Merger(connect, runtastic);

			NodeList connectTracks = merger.getTrackPoints(merger.getConnectDoc());
			NodeList runtasticTracks = merger.getTrackPoints(merger.getRuntasticDoc());

			merger.merge(connectTracks, runtasticTracks);
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

	public File getConnect() {
		return connect;
	}

	public void setConnect(File connect) {
		this.connect = connect;
	}

	public Document getConnectDoc() {
		return connectDoc;
	}

	public void setConnectDoc(Document connectDoc) {
		this.connectDoc = connectDoc;
	}

	public Document getRuntasticDoc() {
		return runtasticDoc;
	}

}
