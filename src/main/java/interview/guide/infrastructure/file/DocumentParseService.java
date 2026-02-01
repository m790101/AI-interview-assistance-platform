package interview.guide.infrastructure.file;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class DocumentParseService {
    private static final int MAX_TEXT_LENGTH = 5 * 1024 * 1024; // 5MB
    private final TextCleaningService textCleaningService;

    public DocumentParseService(TextCleaningService textCleaningService) {
        this.textCleaningService = textCleaningService;
    }


    /**
     *  parse file, get text from file
     *
     * @param file uploaded file（PDF、DOCX、DOC、TXT、MD etc...）
     * @return text from parsing result
     */
    public String parseContent(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("start parsing file: {}", fileName);

        // file is empty
        if (file.isEmpty() || file.getSize() == 0) {
            log.warn("file is empty: {}", fileName);
            return "";
        }

        try (InputStream inputStream = file.getInputStream()) {
            String content = parseContent(inputStream);
            String cleanedContent = textCleaningService.cleanText(content);
            log.info("success，text length is: {} ", cleanedContent.length());
            return cleanedContent;
        } catch (IOException | TikaException | SAXException e) {
            log.error("parsing failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "parsing failed: " + e.getMessage());
        }
    }

    /**
     * parsing byte file
     *
     * @param fileBytes  file in bytes
     * @param fileName origin file name
     * @return text from parsing result
     */
    public String parseContent(byte[] fileBytes, String fileName) {
        log.info("start parsing（from byte in file）: {}", fileName);

        // file is empty
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("byte file is empty: {}", fileName);
            return "";
        }

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            String content = parseContent(inputStream);
            String cleanedContent = textCleaningService.cleanText(content);
            log.info("success，text length is: {} word", cleanedContent.length());
            return cleanedContent;
        } catch (IOException | TikaException | SAXException e) {
            log.error("parsing failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "parsing failed: " + e.getMessage());
        }
    }

    /**
     * parser：default Parser + Context setting
     *
     * custom setting：
     * 1. BodyContentHandler only handle the main part
     * 2. override EmbeddedDocumentExtractor，ignore image, add-on file
     * 3. PDFParserConfig does not parse image and comment
     * 4. 显式指定 Parser 到 Context，增强健壮性
     *
     * @param inputStream input stream
     * @return parsed main part from input
     * @throws IOException     IO exception
     * @throws TikaException   Tika exception
     * @throws SAXException    SAX exception
     */
    private String parseContent(InputStream inputStream) throws IOException, TikaException, SAXException {
        // 1. auto detect init
        AutoDetectParser parser = new AutoDetectParser();

        // 2. only handle text, max length is 5MB
        BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);

        // 3. init meta data
        Metadata metadata = new Metadata();

        // 4. parse conten
        ParseContext context = new ParseContext();

        // 5. assign type to context
        context.set(Parser.class, parser);

        // 6. forbidden embedded doc (avoid image and temp doc path)
        context.set(EmbeddedDocumentExtractor.class, new NoOpEmbeddedDocumentExtractor());

        // 7. for PDF：ignore image，format content
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(false);
        pdfConfig.setSortByPosition(true); // sorted by x-axis/y-axis
        context.set(PDFParserConfig.class, pdfConfig);

        // 8. 执行解析
        parser.parse(inputStream, handler, metadata, context);

        // 9. 返回提取的文本内容
        return handler.toString();
    }

    /**
     * parse file from download file from storage
     *
     * @param storageService  storageService
     * @param storageKey      storage key
     * @param originalFilename  origin name from the file
     * @return parsed text
     */
    public String downloadAndParseContent(FileStorageService storageService, String storageKey, String originalFilename) {
        try {
            byte[] fileBytes = storageService.downloadFile(storageKey);
            if (fileBytes == null || fileBytes.length == 0) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "download failed");
            }
            return parseContent(fileBytes, originalFilename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("download and parsing failed: storageKey={}, error={}", storageKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "download and parsing failed" + e.getMessage());
        }
    }
}
