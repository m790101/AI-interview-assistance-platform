package interview.guide.infrastructure.file;

import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
public class FileHashService {
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    /**
     * calculate file hash key
     * @return
     */
    public String calculateHash(MultipartFile file){
        try{
            return calculateHash(file.getBytes());
        } catch (IOException e) {
            log.error("reading file failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "calculate hash failed");
        }

    }


    /**
     * calculate file hash key
     * @return hex string
     */
    public String calculateHash(byte[] data){
        try{
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("un-supported algo: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "calculate hash failed");
        }

    }


    /**
     * input stream handle
     * @param inputStream input stream
     * @return hex string
     */
    public String calculateHash(InputStream inputStream) {
        try{
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while((bytesRead = inputStream.read(buffer)) != -1){
                digest.update(buffer,0,bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("calculate hash failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "calculate hash failed");
        }
    }


    private String bytesToHex(byte[] bytes){
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for(byte b: bytes){
            result.append(String.format("%02x",b));
        }
        return result.toString();
    }
}
