package com.imagemanager.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.imagemanager.entity.Image;
import com.imagemanager.service.ImageService;
import com.imagemanager.service.McpService;
import com.imagemanager.service.AIService;
import com.imagemanager.common.Result;

import jakarta.servlet.http.HttpServletResponse;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.imagemanager.dto.ImageSearchRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin
public class ImageController {

    @Autowired private ImageService imageService;
    @Autowired private McpService mcpService;
    @Autowired private AIService aiService;

    // 上传图片
    @PostMapping("/upload")
    public Image upload(@RequestParam("file") MultipartFile file, 
                        @RequestAttribute("userId") Long userId) throws IOException {
        // 这里的userId应该通过拦截器从Token中解析并放入 RequestAttribute
        return imageService.upload(file, userId);
    }

    // 搜索图片
    @PostMapping("/search") 
    public Page<Image> search(@RequestBody ImageSearchRequest request, 
                              @RequestAttribute("userId") Long userId) {
        return imageService.searchAdvanced(request, userId);
    }

    // 列图片
    @GetMapping("/list")
    public Page<Image> list(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            @RequestParam(required = false) String keyword,
                            @RequestAttribute("userId") Long userId) {
        return imageService.search(page, size, keyword, userId);
    }

    // 添加标签接口
    @PostMapping("/{imageId}/tags")
    public String addTag(@PathVariable Long imageId, @RequestParam String tagName) {
        imageService.addTag(imageId, tagName);
        return "标签添加成功";
    }

    // 详情接口
    @GetMapping("/{id}")
    public Image getDetail(@PathVariable Long id) {
        Image image = imageService.getDetail(id);
        if (image == null) {
            throw new RuntimeException("图片不存在");
        }
        return image;
    }

    // 删除标签接口
    @DeleteMapping("/{imageId}/tags")
    public String removeTag(@PathVariable Long imageId, @RequestParam String tagName) {
        imageService.removeTag(imageId, tagName);
        return "标签删除成功";
    }

    // 重命名接口
    @PostMapping("/{imageId}/rename")
    public String rename(@PathVariable Long imageId, @RequestParam String newName) {
        imageService.renameImage(imageId, newName);
        return "重命名成功";
    }

    // 下载接口
    // 不需要返回String，直接操作Response流
    @GetMapping("/{imageId}/download")
    public void download(@PathVariable Long imageId, HttpServletResponse response) {
        imageService.downloadImage(imageId, response);
    }
    
    // 图片裁剪
    @PostMapping("/{id}/crop")
    public String crop(@PathVariable Long id, @RequestBody Map<String, Integer> rect) throws IOException {
        String srcPath = "/data/images/original/test.jpg"; 
        String outPath = "/data/images/original/test_crop.jpg";
        
        Thumbnails.of(srcPath)
                .sourceRegion(rect.get("x"), rect.get("y"), rect.get("w"), rect.get("h"))
                .size(rect.get("w"), rect.get("h"))
                .toFile(outPath);
        return "Crop Success: " + outPath;
    }

    // 批量删除接口
    @PostMapping("/batch-delete")
    public String deleteBatch(@RequestBody List<Long> imageIds, @RequestAttribute("userId") Long userId) {
        imageService.deleteBatch(imageIds, userId);
        return "批量删除成功";
    }

    // 更新图片内容接口
    @PostMapping("/{imageId}/content")
    public String updateImageContent(
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("userId") Long userId
    ) throws IOException {
        imageService.updateImageContent(imageId, file, userId);
        return "图片编辑保存成功";
    }

    // AI分析接口
    @PostMapping("/{id}/analyze")
    public Result analyzeImage(@PathVariable Long id) {
        // 查图片信息
        Image image = imageService.getById(id);
        if (image == null) {
            return Result.error("图片不存在");
        }
        // 获取图片本地绝对路径
        String fullPath = image.getStoragePath(); 
        // 调用AI分析
        List<String> newTags = aiService.analyzeImage(fullPath);
        if (newTags == null || newTags.isEmpty()) {
            return Result.error("未能识别出有效内容或AI服务异常");
        }
        // 将新标签批量保存到数据库
        imageService.updateImageTags(id, newTags); 
        // 返回新标签给前端
        return Result.success(newTags);
    }
}