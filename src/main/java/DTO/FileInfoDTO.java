package DTO;

import lombok.Data;

@Data
public class FileInfoDTO { // 可选的文件信息 DTO
    private String fileName;
    private long fileSize;
    private String fileUrl; // 下载/预览 URL
    // Getters and Setters...
}
