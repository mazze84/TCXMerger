package logic;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Merger {
	private File connect = null;
	private File runtastic = null;

	private Document connectDoc;
	private Document runtasticDoc;

	public enum GarminXML {

		ACTIVITIES("Activities"), ACTIVITY("Activity"), LAP("Lap"), TRACK("Track"), TRACKPOINT("Trackpoint"), TIME(
				"Time"), MAX_HEARTRATE("MaximumHeartRateBpm"), AVG_HEARTRATE("AverageHeartRateBpm"), HEARTRATE(
						"HeartRateBpm"), CALORIES("Calories"), DISTANCE("DistanceMeters");

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

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		this.connectDoc = builder.parse(this.connect);
		this.runtasticDoc = builder.parse(this.runtastic);
	}

	private Node getSubNode(Node parrent, String nodeName) {
		if (parrent.hasChildNodes()) {
			NodeList childNodes = parrent.getChildNodes();
			for (int index = 0; index < childNodes.getLength(); index++) {
				if (childNodes.item(index).getNodeName().equals(nodeName)) {
					return childNodes.item(index);
				}
			}
		}
		return null;
	}

	/**
	 * merges the two trackpoints.
	 * 
	 * @param connect
	 *            the tracklist of the connect tcx file
	 * @param runtastic
	 *            the tracklist of the runtastic tcx file
	 */
	public void merge(NodeList connect, NodeList runtastic) {
		int runtasticLength = runtastic.getLength();

		int index = 0;
		for (Node connectNode = connect.item(0); connectNode != null; connectNode = connectNode.getNextSibling()) {

			Node timeConnect = getSubNode(connectNode, GarminXML.TIME.getElementName());
			if (timeConnect != null) {

				for (; index < runtasticLength; index++) {
					Node runtasticNode = runtastic.item(index);

					Node timeRuntastic = getSubNode(runtasticNode, GarminXML.TIME.getElementName());

					if (timeRuntastic != null) {
						String value = timeConnect.getNodeValue();
						int isPrior = checkTime(timeConnect.getTextContent(), timeRuntastic.getTextContent());
						System.out.println(timeConnect.getTextContent() + " " + timeRuntastic.getTextContent());

						if (isPrior == 1) {
							// append node prior to other node
							Node newClone = runtasticNode.cloneNode(true);
							connectNode.getOwnerDocument().adoptNode(newClone);
							connectNode.getParentNode().insertBefore(newClone, connectNode);
						} else if (isPrior == 0) {
							// merge node into other node
							mergeInto(connectNode, runtasticNode);
						} else {
							// jump to next node
							break;
						}

					}
				}
			}

		}

	}

	public void removeZeroSpeed(NodeList nodeList) {
		for (int index = 0; index < nodeList.getLength(); index++) {
			Node distance = getSubNode(nodeList.item(index), GarminXML.DISTANCE.getElementName());
			if (distance != null) {
				if (Double.parseDouble(distance.getTextContent()) == 0.0) {
					nodeList.item(index).removeChild(distance);
				}
			}
		}
	}

	private boolean isElement(String element, String[] list) {
		for (String item : list) {
			if (item.equals(element)) {
				return true;
			}
		}

		return false;
	}

	private void mergeInto(Node parrent, Node toInsert) {
		NodeList toInsertChilds = toInsert.getChildNodes();
		// TODO: check if the same child element is already existent
		// do not add the existing element

		NodeList nodeChilds = parrent.getChildNodes();
		String[] childNames = new String[nodeChilds.getLength()];
		for (int index = 0; index < nodeChilds.getLength(); index++) {
			childNames[index] = nodeChilds.item(index).getNodeName();
		}

		for (int index = 0; index < toInsertChilds.getLength(); index++) {
			if (toInsertChilds.item(index).getNodeName().equals(GarminXML.TIME.getElementName())) {
				continue;
			}

			Node clone = toInsertChilds.item(index).cloneNode(true);
			if (!isElement(clone.getNodeName(), childNames)) {
				if (!clone.hasChildNodes() && !clone.getTextContent().trim().isEmpty()) {
					try {
						if (Double.parseDouble(clone.getNodeValue()) != 0.0) {
							parrent.getOwnerDocument().adoptNode(clone);
							parrent.appendChild(clone);
						}
					} catch (NumberFormatException e) {
						;
					}
				} else {
					parrent.getOwnerDocument().adoptNode(clone);
					parrent.appendChild(clone);
				}
			} else {
				Node node = getSubNode(parrent, clone.getNodeName());
				if (node != null && !node.hasChildNodes()) {
					try {
						if (Double.parseDouble(node.getTextContent()) == 0.0) {
							parrent.getOwnerDocument().adoptNode(clone);
							parrent.replaceChild(clone, node);
						}
					} catch (NumberFormatException e) {
						;
					}
				}
			}
		}
	}

	public NodeList getTrackPoints(Document doc) {
		if (doc == null) {
			return null;
		}
		Node activities = getSubNode(doc.getDocumentElement(), GarminXML.ACTIVITIES.getElementName());
		if (activities != null) {
			Node activity = getSubNode(activities, GarminXML.ACTIVITY.getElementName());
			if (activity != null) {
				Node lap = getSubNode(activity, GarminXML.LAP.getElementName());
				if (lap != null) {
					return getSubNode(lap, GarminXML.TRACK.getElementName()).getChildNodes();
				}
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

	private void writeFile(File outputFile) throws TransformerException {
		// TODO: write new tcx file
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();

		DOMSource source = new DOMSource(connectDoc);
		StreamResult result = new StreamResult(outputFile);
		transformer.transform(source, result);
	}

	public static void main(String args[]) {

		File connect = new File("activity_1229268573.tcx");
		File runtastic = new File("runtastic_20160625_1142_Radfahren.tcx");
		try {
			Merger merger = new Merger(connect, runtastic);

			NodeList connectTracks = merger.getTrackPoints(merger.getConnectDoc());
			NodeList runtasticTracks = merger.getTrackPoints(merger.getRuntasticDoc());

			merger.removeZeroSpeed(connectTracks);

			merger.merge(connectTracks, runtasticTracks);

			merger.writeFile(new File("merged.tcx"));
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
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
