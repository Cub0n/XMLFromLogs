
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

final class FileProcessor implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(FileProcessor.class.getName());

	private final String xmlString;

	private final String filename;

	FileProcessor(final String xml, final String name) {
		xmlString = xml;
		filename = name;
	}

	private void writeToFile() {

		try {
			// Parse the given input
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, StringUtils.EMPTY);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, StringUtils.EMPTY);
			factory.setExpandEntityReferences(false);

			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc = builder.parse(new InputSource(new StringReader(xmlString)));

			// Write out to file in pretty print
			final File file = new File(filename + ".xml");
			if (!file.exists() || file.canWrite()) {
				final Result gevoFile = new StreamResult(file);
				final TransformerFactory transfomerFactory = TransformerFactory.newInstance();
				transfomerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, StringUtils.EMPTY);
				transfomerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, StringUtils.EMPTY);

				final Transformer transformer = transfomerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
				transformer.setOutputProperty(OutputKeys.ENCODING, Charset.defaultCharset().name());
				transformer.setOutputProperty(OutputKeys.METHOD, "xml");
				transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
				transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");

				final DOMSource source = new DOMSource(doc);
				transformer.transform(source, gevoFile);
			}
		}
		catch (ParserConfigurationException | SAXException | IOException | TransformerException ex) {
			LOGGER.log(Level.WARNING, "Error at: {0}", ex.getMessage());
		}

		LOGGER.log(Level.INFO, "File created: {0}", filename);
	}

	@Override
	public void run() {
		writeToFile();
	}
}
