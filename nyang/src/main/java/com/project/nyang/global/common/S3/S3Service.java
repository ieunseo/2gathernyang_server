package com.project.nyang.global.common.S3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * S3 서비스입니다.
 *
 * @author : 선순주
 * @fileName : S3Service
 * @since : 2025-07-10
 */
@Service
@RequiredArgsConstructor
public class S3Service {
    @Value("${storage.bucket}")
    private String bucket;

    @Value("${storage.endpoint}")
    private String endpoint;

    private final S3Client s3Client;

    public List<String> uploadFile(List<MultipartFile> multipartFiles){
        List<String> fileNameList = new ArrayList<>();

        // forEach 구문을 통해 multipartFiles 리스트로 넘어온 파일들을 순차적으로 fileNameList 에 추가
        multipartFiles.forEach(file -> {
            String fileName = createFileName(file.getOriginalFilename());
            // 공개 여부는 Supabase 버킷 설정(Public)으로 정하므로 ACL은 넣지 않는다
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            try(InputStream inputStream = file.getInputStream()){
                s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            } catch (IOException e){
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");
            }

            fileNameList.add(publicUrl(fileName));

        });

        return fileNameList;
    }

    // S3 접속 주소(.../storage/v1/s3)를 공개 이미지 주소(.../storage/v1/object/public/버킷/파일명)로 바꾼다
    private String publicUrl(String fileName) {
        return endpoint.replaceFirst("/s3/?$", "/object/public/") + bucket + "/" + fileName;
    }

    // 이미지 주소에서 저장소 안의 파일명만 꺼낸다 (마지막 '/' 뒤)
    public static String extractFileName(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }

    // 파일명을 난수화하기 위해 UUID 를 활용하여 난수를 돌린다.
    public String createFileName(String fileName){
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }

    //  "."의 존재 유무만 판단
    private String getFileExtension(String fileName){
        try{
            return fileName.substring(fileName.lastIndexOf( "."));
        } catch (StringIndexOutOfBoundsException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일" + fileName + ") 입니다.");
        }
    }


    public void deleteFile(String fileName){
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(fileName).build());
//        System.out.println(bucket);
    }
}