package interview.guide.infrastructure.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class NoOpEmbeddedDocumentExtractor implements EmbeddedDocumentExtractor {

    @Override
    public boolean shouldParseEmbedded(Metadata metadata) {
        String resourceName = metadata.get("resourceName");
        if(resourceName != null){
            log.debug("Skip embedded document: {}", resourceName);
        }
        return false;
    }

    @Override
    public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml) throws SAXException, IOException {
    }
}
