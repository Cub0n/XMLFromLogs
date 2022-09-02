
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public final class Extractor {

	private static final String BEGIN_TAG = "<soap:Envelope>";

	private static final String END_TAG = "</soap:Envelope>";

	private static ExecutorService executor = null;

	private static int counter = 0;

	private static StringBuilder strBuilder = null;

	private static String filename = null;

	private Extractor() {
		//
	}

	/**
	 * Function for finding XML's in a file.
	 *
	 * Reading file line by line is recommended, especially when the file is huge. (NOT using
	 * {@link Files#readAllLines(java.nio.file.Path)} or IOUtils.readLines(...)). This should avoid huge memory
	 * allocation during reading.
	 *
	 * Lazy loading with stream of Strings. See {@link Files#lines(java.nio.file.Path)}
	 *
	 * @param file
	 * @param name
	 * @throws IOException
	 */
	public static void extractXMLs(final File file, final String name) throws IOException {

		filename = name;
		executor = Executors.newFixedThreadPool(32);

		// old implementation:
		//
		//		try (BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset())) {
		//
		//			String line = null;
		//			while ((line = br.readLine()) != null) {
		//				extractXML(line);
		//			}
		//		}

		try (Stream<String> lines = Files.lines(file.toPath())) {
			lines.forEach(Extractor::extractXML);
		}
		finally {
			executor.shutdown();
			try {
				executor.awaitTermination(120, TimeUnit.SECONDS);
			}
			catch (final InterruptedException exception) {
				// Restore interrupted state...
				Thread.currentThread().interrupt();
				executor.shutdownNow();
			}
		}
	}

	/**
	 * <pre>
	 * Parsing a line. In one line could be many start/end-tags.
	 * 
	 * Main idea: 
	 * - Everytime BEGIN_TAG is found reset and collect the input starting with BEGIN_TAG
	 * - If END_TAG is found stop the collection and write result.
	 * - The rest of the input can be analyzed by recursively calling the same function with smaller part of input.
	 * 
	 * Distinguish following cases:
	 * 
	 * Case 1a: 
	 * 			Line contains at minimum one BEGIN_TAG and no END_TAG 
	 * 			--> Start collecting, inspect rest of the line starting after BEGIN_TAG
	 * Case 1b: 
	 * 			Line contains at minimum one BEGIN_TAG and END_TAG. BEGIN_TAG before first END_TAG.
	 * 			(Takes care of: <END_TAG> before <BEGIN_TAG> in input)
	 * 			--> Start collecting, inspect rest of the line starting after BEGIN_TAG
	 * 			--> Else continue with other cases (Case 2a)
	 * Case 2a: 
	 * 			END_TAG is found and collection started
	 * 			--> Stop collecting, write to file, inspect rest of the line starting after END_TAG 
	 * 			(Takes care of: New <BEGIN_TAG> after <END_TAG> in input)
	 * Case 2b:
	 * 			END_TAG is found, but no collection happened. No BEGIN_TAG found beforehand
	 * 			--> Inspect rest of the line starting after END_TAG
	 * Case 3:
	 * 			Neither BEGIN_TAG nor END_TAG found. If collecting already started, collect the input, else continue without collecting.
	 * 			- Input belongs to XML --> collect input
	 * 			- Random data between two XMLs --> don't collect (No BEGIN_TAG started collecting)
	 * </pre>
	 *
	 * @param input
	 *            - Stringline
	 */
	private static void extractXML(final String input) {

		final int openStringPos = StringUtils.indexOf(input, BEGIN_TAG);

		final int endStringPos = StringUtils.indexOf(input, END_TAG);

		if ((openStringPos >= 0) && ((endStringPos < 0) || (openStringPos < endStringPos))) {
			strBuilder = new StringBuilder(BEGIN_TAG);
			extractXML(StringUtils.substringAfter(input, BEGIN_TAG));
		}

		else if (endStringPos >= 0) {
			if (strBuilder != null) {
				strBuilder.append(StringUtils.substringBefore(input, END_TAG));
				strBuilder.append(END_TAG);
				writeOut(strBuilder.toString());
				strBuilder = null;
			}
			extractXML(StringUtils.substringAfter(input, END_TAG));
		}

		else if ((openStringPos < 0) && (endStringPos < 0) && (strBuilder != null)) {
			strBuilder.append(input);
		}
	}

	private static void writeOut(final String xml) {
		counter++;
		executor.execute(new FileProcessor(xml, filename + counter));
	}
}
