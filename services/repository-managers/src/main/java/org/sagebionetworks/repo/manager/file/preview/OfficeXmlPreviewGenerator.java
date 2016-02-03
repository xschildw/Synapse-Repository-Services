package org.sagebionetworks.repo.manager.file.preview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;

import com.google.common.collect.ImmutableSet;

import org.docx4j.Docx4J;
import org.docx4j.convert.out.FOSettings;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 * Generates previews for image content types.
 * 
 * @author John
 * 
 */
public class OfficeXmlPreviewGenerator implements PreviewGenerator {

	private static Log logger = LogFactory.getLog(OfficeXmlPreviewGenerator.class);

	public static final Set<String> OFFICE_MIME_TYPES = ImmutableSet
			.<String> builder()
			.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document"/*,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					"application/vnd.openxmlformats-officedocument.presentationml.presentation"*/)
			.build();

	public static final String ENCODING = "UTF-8";

	public OfficeXmlPreviewGenerator() {
	}

	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException, RuntimeException {
		try {
	        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(from);

	        FOSettings settings = Docx4J.createFOSettings();
	        settings.setWmlPackage(wordMLPackage);
	        settings.setApacheFopMime("image/png");
	        Docx4J.toFO(settings, to, Docx4J.FLAG_NONE);
	        
		} catch (Docx4JException e) {
			throw new RuntimeException("Docx4JException caught while generating the preview.");
		}
		
		PreviewOutputMetadata pom = new PreviewOutputMetadata("image/png", ".png");
		return pom;

	}

	@Override
	public boolean supportsContentType(String contentType, String extension) {
		if(!StackConfiguration.singleton().getOpenOfficeImageMagicePreviewsEnabled()){
			return false;
		}
		return OFFICE_MIME_TYPES.contains(contentType);
	}

	@Override
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize) {
		// whole file is read into memory pretty much
		return contentSize * 2;
	}

}
