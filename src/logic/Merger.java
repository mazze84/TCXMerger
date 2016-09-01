package logic;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
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
						"HeartRateBpm"), CALORIES("Calories"), ALTITUDE("AltitudeMeters"), DISTANCE(
								"DistanceMeters"), TOTAL_TIME("TotalTimeSeconds"), MAX_SPEED("MaximumSpeed"), CADENCE(
										"Cadence"), VALUE("Value"), POSITION("Position"), EXTENSIONS("Extensions");

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

	public Node getSubNode(Node parent, String nodeName) {
		if (parent.hasChildNodes()) {
			NodeList childNodes = parent.getChildNodes();
			for (int index = 0; index < childNodes.getLength(); index++) {
				if (childNodes.item(index).getNodeName().equals(nodeName)) {
					return childNodes.item(index);
				}
			}
		}
		return null;
	}

	private List<Node> getNodeList(Node parent, String nodeName) {
		List<Node> nodes = new ArrayList<Node>();
		if (parent.hasChildNodes()) {
			NodeList childNodes = parent.getChildNodes();
			for (int index = 0; index < childNodes.getLength(); index++) {
				if (childNodes.item(index).getNodeName().equals(nodeName)) {
					nodes.add(childNodes.item(index));
				}
			}
		}
		return nodes;
	}

	public List<Node> getLaps(Document document) {
		if (document == null) {
			return null;
		}

		Node activities = getSubNode(document.getDocumentElement(), GarminXML.ACTIVITIES.getElementName());
		if (activities != null) {
			Node activity = getSubNode(activities, GarminXML.ACTIVITY.getElementName());
			if (activity != null) {
				return getNodeList(activity, GarminXML.LAP.getElementName());

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
						int isPrior = checkTime(timeConnect.getTextContent(), timeRuntastic.getTextContent());
						System.out.println(timeConnect.getTextContent() + " " + timeRuntastic.getTextContent());

						if (isPrior == 1) {
							// append node prior to other node
							adoptNode(connectNode, runtasticNode, false);
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

		// if there are nodes left append them to the end of the list
		if (index < runtasticLength) {
			Node connectParent = connect.item(0).getParentNode();
			for (; index < runtasticLength; index++) {
				appendNode(connectParent, runtastic.item(index));
			}
		}

	}

	public void removeZeroDistance(NodeList nodeList) {
		for (int index = 0; index < nodeList.getLength(); index++) {
			Node distance = getSubNode(nodeList.item(index), GarminXML.DISTANCE.getElementName());
			if (distance != null) {
				if (Double.parseDouble(distance.getTextContent()) == 0.0) {
					nodeList.item(index).removeChild(distance);
				}
			}
		}
	}

	private int compareLaps(List<Node> nodes1, List<Node> nodes2) {
		return nodes1.size() - nodes2.size();
	}

	public void mergeLapInfo(Node lap, Node lap2) {
		if (!lap.getNodeName().equals(GarminXML.LAP.getElementName())
				&& !lap2.getNodeName().equals(GarminXML.LAP.getElementName())) {
			return;
		}

		mergeTotalTime(getSubNode(lap, GarminXML.TOTAL_TIME.getElementName()),
				getSubNode(lap2, GarminXML.TOTAL_TIME.getElementName()));

		mergeDistance(getSubNode(lap, GarminXML.DISTANCE.getElementName()),
				getSubNode(lap2, GarminXML.DISTANCE.getElementName()));

		mergeHeartRate(getSubNode(lap, GarminXML.MAX_HEARTRATE.getElementName()),
				getSubNode(lap2, GarminXML.MAX_HEARTRATE.getElementName()));

		mergeHeartRate(getSubNode(lap, GarminXML.AVG_HEARTRATE.getElementName()),
				getSubNode(lap2, GarminXML.AVG_HEARTRATE.getElementName()));

	}

	private boolean mergeDistance(Node distanceNode, Node distanceNode2) {
		if (distanceNode2 != null) {
			BigDecimal distance2 = new BigDecimal(distanceNode2.getTextContent());

			if (distanceNode != null) {
				BigDecimal distance = new BigDecimal(distanceNode.getTextContent());

				if (distance.compareTo(distance2) == -1) {
					adoptNode(distanceNode, distanceNode2, true);

					return true;
				}
			} else {
				adoptNode(distanceNode, distanceNode2, true);
				return true;
			}

		}
		return false;
	}

	private boolean mergeTotalTime(Node totalTimeNode, Node totalTimeNode2) {
		if (totalTimeNode2 != null) {
			BigDecimal totalTime2 = new BigDecimal(totalTimeNode2.getTextContent());

			if (totalTimeNode != null) {
				BigDecimal totalTime = new BigDecimal(totalTimeNode.getTextContent());

				if (totalTime.compareTo(totalTime2) == -1) {
					adoptNode(totalTimeNode, totalTimeNode2, true);
					return true;
				}
			} else {
				adoptNode(totalTimeNode, totalTimeNode2, true);
				return true;
			}
		}
		return false;
	}

	private boolean mergeHeartRate(Node heartrateNode, Node heartrateNode2) {
		if (heartrateNode2 != null) {
			Node value = getSubNode(heartrateNode2, GarminXML.VALUE.getElementName());
			if (value != null) {
				BigDecimal heartrate2 = new BigDecimal(value.getTextContent());

				if (heartrateNode != null) {
					value = getSubNode(heartrateNode, GarminXML.VALUE.getElementName());
					if (value != null) {
						BigDecimal heartrate = new BigDecimal(value.getTextContent());

						if (heartrate.compareTo(heartrate2) == -1) {
							adoptNode(heartrateNode, heartrateNode2, true);
							return true;

						}
					}
				} else {
					adoptNode(heartrateNode, heartrateNode2, true);
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * 
	 * @param node1
	 * @param node2
	 * @param replace
	 *            if true the node will be replaced, if false the node will be
	 *            inserted before
	 */
	private void adoptNode(Node node1, Node node2, boolean replace) {
		if (node1 != null && node2 != null) {
			Node newNode = node2.cloneNode(true);
			node1.getOwnerDocument().adoptNode(newNode);
			System.out.println(nodeToString(newNode));
			if (replace) {
				node1.getParentNode().replaceChild(newNode, node1);
			} else {
				node1.getParentNode().insertBefore(newNode, node1);
			}

		}
	}

	private void appendNode(Node parent, Node toAppend) {
		if (parent != null && toAppend != null) {
			Node newNode = toAppend.cloneNode(true);
			parent.getOwnerDocument().adoptNode(newNode);

			parent.appendChild(newNode);
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

	/**
	 * Checks if the Node has a Extensions Node
	 * 
	 * @param node
	 *            Trackpoint Node
	 * @return returns the Extension Node
	 */
	private Node hasExtensionNode(Node node) {
		if (GarminXML.TRACKPOINT.getElementName().equals(node.getNodeName())) {
			NodeList children = node.getChildNodes();
			for (int index = 0; index <= children.getLength(); index++) {
				if (GarminXML.EXTENSIONS.getElementName().equals(children.item(index).getNodeName())) {
					return children.item(index);
				}
			}
		}
		return null;
	}

	/**
	 * distance and altitude is usually more precise with GPS. GPS will have the
	 * Position sub-node.
	 * 
	 * @param node
	 *            Trackpoint node to check
	 * @return Returns true if the Trackpoint node has a Position node
	 */
	private boolean checkPrecision(Node node) {
		if (node.getNodeName().equals(GarminXML.TRACKPOINT.getElementName())) {
			if (getSubNode(node, GarminXML.POSITION.getElementName()) != null) {
				return true;
			}
		}
		return false;
	}

	private void mergeInto(Node parent, Node toInsert) {
		NodeList toInsertChilds = toInsert.getChildNodes();
		// TODO: check if the same child element is already existent
		// do not add the existing element

		for (int index = 0; index < toInsertChilds.getLength(); index++) {
			Node node = toInsertChilds.item(index);

			// time is equal, skip it
			if (node.getNodeName().equals(GarminXML.TIME.getElementName())) {
				continue;
			}

			// if gps node check
			if (node.getNodeName().equals(GarminXML.ALTITUDE.getElementName())
					|| node.getNodeName().equals(GarminXML.DISTANCE.getElementName())) {
				if (checkPrecision(node.getParentNode())) {
					adoptNode(getSubNode(parent, node.getNodeName()), node, true);
					continue;
				}
			}

			// if node is not in the parent node
			Node newNode = getSubNode(parent, node.getNodeName());
			if (newNode == null) {
				if (!node.hasChildNodes() && !node.getTextContent().trim().isEmpty()) {
					try {
						if (Double.parseDouble(node.getTextContent()) != 0.0) {
							adoptNode(parent, node, true);
						}
					} catch (NumberFormatException e) {
						;
					}
				} else {
					adoptNode(parent, node, true);
				}
			} else {
				if (newNode != null && !newNode.hasChildNodes()) {
					try {
						if (Double.parseDouble(newNode.getTextContent()) == 0.0) {
							parent.getOwnerDocument().adoptNode(node);
							parent.replaceChild(node, newNode);
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

	public void writeFile(File outputFile) throws TransformerException {
		// TODO: write new tcx file
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();

		DOMSource source = new DOMSource(connectDoc);
		StreamResult result = new StreamResult(outputFile);
		transformer.transform(source, result);
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

	public String nodeToString(Node node) {
		StringBuilder nodeString = new StringBuilder();

		nodeString.append("<");
		nodeString.append(node.getNodeName());

		NamedNodeMap attributes = node.getAttributes();

		for (int index = 0; index < attributes.getLength(); index++) {
			nodeString.append(attributes.item(index) + " ");
		}

		nodeString.append(">" + node.getNodeValue() + " " + node.getTextContent());

		return nodeString.toString();

	}

}
