
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExtractorTest {

	private static final String TEST_FILE = "test.log";

	@Test
	public void simpleFile() throws IOException {
		File testFile = new File(getClass().getClassLoader().getResource(TEST_FILE).getFile());

		Extractor.extractXMLs(testFile, "TEST");

		assertFile("TEST1");
		assertFile("TEST2");
		assertFile("TEST3");
		assertFile("TEST4");
		assertFile("TEST5");
		assertFile("TEST6");
		assertFile("TEST7");
		assertFile("TEST8");
		assertFile("TEST9");
	}

	private static void assertFile(final String outputFileName) throws IOException {
		Path path = new File(outputFileName + ".xml").toPath();
		List<String> lines = Files.readAllLines(path);

		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line);
		}

		Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><soap:Envelope>    <test>"
				+ outputFileName
				+ "</test></soap:Envelope>", sb.toString());

		// CleanUp
		Files.delete(path);
	}

}
