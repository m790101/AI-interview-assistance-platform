package interview.guide.infrastructure.file;

import interview.guide.common.config.S3Config;
import interview.guide.common.config.StorageConfigProperties;
import interview.guide.exception.BusinessException;
import interview.guide.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final S3Client s3Client;
    private final StorageConfigProperties storageConfig;


    /**
    * upload resume file
    */
    public String uploadResume(MultipartFile file){
        return uploadFile(file, "resume");
    }


    /**
     * download resume file
     */
    public byte[] downloadResume(String fileKey){
        // check file exists
        if (!fileExists(fileKey)) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "文件不存在: " + fileKey);
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            return s3Client.getObjectAsBytes(getRequest).asByteArray();
        } catch (S3Exception e) {
            log.error("下载文件失败: {} - {}", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "文件下载失败: " + e.getMessage());
        }
    }

    /**
     *  delete resume
     */
    public void deleteResume(String fileKey){
        deleteFile(fileKey);
    }

    /**
     * upload knowledge
     */
    public String uploadKnowledgeBase(MultipartFile file){
        return uploadFile(file, "knowledgebase");
    }


    /**
     * delete knowledge
     */
    public void deleteKnowledgeBase(String fileKey){
        deleteFile(fileKey);
    }


    /**
     * get file url
     */
    public String getFileUrl(String fileKey){
        return String.format("%s/%s/%s", storageConfig.getEndpoint(), storageConfig.getBucket(), fileKey);
    }

    /**
     * download file
     */
    public byte[] downloadFile(String fileKey){
       if(!fileExists(fileKey)){
           throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "file is not found" + fileKey);
       }
       try{
           GetObjectRequest getRequest = GetObjectRequest.builder()
                   .bucket(storageConfig.getBucket())
                   .key(fileKey)
                   .build();
           return s3Client.getObjectAsBytes(getRequest).asByteArray();
       } catch (S3Exception e){
           log.error("download failed: {} - {}", fileKey, e.getMessage(), e);
           throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "file is not found" + e.getMessage());
       }
    }

    /**
     * upload file
     */
    public String uploadFile(MultipartFile file, String prefix){
        String originFilename = file.getOriginalFilename();
        String fileKey = generateFileKey(originFilename, prefix);

        try{
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("file upload successfully: {} - {} ", originFilename, fileKey);
            return fileKey;
        } catch (IOException e) {
            log.error("reading upload file is failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, "reading file is failed");
        } catch(S3Exception e){
            log.error("upload to RustFS is failed", e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAILED, "upload file is failed");
        }
    }


    /**
     * fileExists
     */
    public boolean fileExists(String fileKey){
        try{
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (S3Exception e) {
            log.warn("fileExist error {} - {}", fileKey, e.getMessage());
            return false;
        }
    }

    /**
     * get file size
     */
    public long getFileSize(String fileKey){
        try{
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .key(fileKey)
                    .build();
            return s3Client.headObject(headRequest).contentLength();
        } catch (S3Exception e) {
            log.error("get file size fail: {} - {}", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "get file size fail");
        }
    }

    /**
     * delete file
     */
    public void deleteFile(String fileKey){
       if(fileKey == null || fileKey.isEmpty()){
           log.debug("file key is empty");
           return;
       }
       if(!fileExists(fileKey)){
           log.debug("file not exist, ignore");
           return;
       }

       try {
           DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                   .bucket(storageConfig.getBucket())
                   .key(fileKey)
                   .build();
           s3Client.deleteObject(deleteRequest);
           log.info("delete success");
       } catch (S3Exception e) {
           log.error("delete file fail: {} - {}", fileKey, e.getMessage(), e);
           throw new BusinessException(ErrorCode.STORAGE_DELETE_FAILED, "get file size fail");
       }
    }


    /**
     *  check if bucket exists
     */
    public void checkBucketExists(){
        try{
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .build();
            s3Client.headBucket(headRequest);
            log.info("bucket exist: {}", storageConfig.getBucket());
        } catch(NoSuchBucketException e){
            log.info("存储桶不存在，正在创建: {}", storageConfig.getBucket());
            CreateBucketRequest createRequest = CreateBucketRequest.builder()
                    .bucket(storageConfig.getBucket())
                    .build();
            s3Client.createBucket(createRequest);
            log.info("存储桶创建成功: {}", storageConfig.getBucket());
        } catch(S3Exception e){
            log.error("create bucket fail: {}", e.getMessage(),e);
        }
    }

    /**
     * generate file key
     */
    private String generateFileKey(String originalFileName, String prefix){
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().substring(0,8);
        String safeName = sanitizeFilename(originalFileName);
        return String.format("%s/%s/%s_%s",prefix,datePath,uuid, safeName);
    }

    private String sanitizeFilename(String filename) {
        if (filename == null)
            return "unknown";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
