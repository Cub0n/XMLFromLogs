
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

public final class Extractor {

	private static final String BEGIN_TAG = "<soap:Envelope>";

	private static final String END_TAG = "</soap:Envelope>";

	private static final int END_TAG_LENGTH = END_TAG.length();

	private static ExecutorService executor = null;

	private static int counter = 0;

	private static StringBuilder strBuilder = null;

	/**
	 * Lond descriptoion ....
	 *
	 * @param input
	 * @param name
	 */
	private static void extractXML(final String input, final String name) {

		final int openStringPos = StringUtils.indexOf(input, BEGIN_TAG);

		final int endStringPos = StringUtils.indexOf(input, END_TAG);

		if ((strBuilder == null) && (openStringPos >= 0) && (endStringPos >= 0)) {
			writeOut(StringUtils.substring(input, openStringPos, endStringPos + END_TAG_LENGTH), name);
			extractXML(StringUtils.substring(input, (endStringPos + END_TAG_LENGTH)), name);
		} else if ((strBuilder == null) && (openStringPos >= 0) && (endStringPos < 0)) {
			strBuilder = new StringBuilder(input.substring(openStringPos));
		} else if ((strBuilder != null) && (endStringPos >= 0)
				&& ((openStringPos < 0) || ((openStringPos >= 0) && (endStringPos < openStringPos)))) {
			strBuilder.append(StringUtils.substring(input, 0, endStringPos + END_TAG_LENGTH));
			writeOut(strBuilder.toString(), name);
			strBuilder = null;
			extractXML(StringUtils.substring(input, (endStringPos + END_TAG_LENGTH)), name);
		} else if ((strBuilder != null) && (openStringPos < 0) && (endStringPos < 0)) {
			strBuilder.append(input);
		}
	}

	/**
	 * Function for finding XML's in a file.
	 *
	 * Reading file line by line is recommended, especially when the file is huge.
	 * (NOT using {@link Files#readAllLines(java.nio.file.Path)} or
	 * IOUtils.readLines(...)). This should avoid huge memory allocation during
	 * reading.
	 *
	 * Lazy loading with stream of Strings is also possible
	 * {@link Files#lines(java.nio.file.Path)}
	 *
	 * @param file
	 * @param name
	 * @throws IOException
	 */
	public static void extractXMLs(final File file, final String name) throws IOException {

		executor = Executors.newFixedThreadPool(32);

		try (BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset())) {

			String line = null;
			while ((line = br.readLine()) != null) {
				extractXML(line, name);
			}
		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(120, TimeUnit.SECONDS);
			} catch (final InterruptedException exception) {
				// Restore interrupted state...
				Thread.currentThread().interrupt();
				executor.shutdownNow();
			}
		}
	}

	private static void writeOut(final String xml, final String name) {
		counter++;
		executor.execute(new FileProcessor(xml, name + counter));
	}
}