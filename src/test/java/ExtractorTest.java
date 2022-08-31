

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class ExtractorTest {

	@Test
	public void simpleFile() throws IOException, URISyntaxException {
		URL path = ClassLoader.getSystemResource("test.log");
		File file = new File(path.toURI());

		Extractor.extractXMLs(file, "test");

		//		String str =
		//				IOUtils.resourceToString("simple1.xml", Charset.defaultCharset(), ExtractorTest.class.getClassLoader());
		//
		//		Assertions.assertEquals("", StringUtils.chomp(str));

	}

}
