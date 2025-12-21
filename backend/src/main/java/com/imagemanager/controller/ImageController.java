package com.imagemanager.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.imagemanager.entity.Image;
import com.imagemanager.service.ImageService;
import com.imagemanager.service.McpService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.imagemanager.dto.ImageSearchRequest;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@CrossOrigin
public class ImageController {

    @Autowired private ImageService imageService;
    @Autowired private McpService mcpService;

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

    // 搜索图片
    @GetMapping("/list")
    public Page<Image> list(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            @RequestParam(required = false) String keyword,
                            @RequestAttribute("userId") Long userId) {
        return imageService.search(page, size, keyword, userId);
    }
    
    // 图片裁剪
    @PostMapping("/{id}/crop")
    public String crop(@PathVariable Long id, @RequestBody Map<String, Integer> rect) throws IOException {
        // 简化演示：实际应先查 DB 找路径
        String srcPath = "/data/images/original/test.jpg"; 
        String outPath = "/data/images/original/test_crop.jpg";
        
        Thumbnails.of(srcPath)
                .sourceRegion(rect.get("x"), rect.get("y"), rect.get("w"), rect.get("h"))
                .size(rect.get("w"), rect.get("h"))
                .toFile(outPath);
        return "Crop Success: " + outPath;
    }

    // MCP接口,大模型对话检索
    @PostMapping("/mcp/search")
    public Object mcpSearch(@RequestBody Map<String, String> body) {
        String naturalLanguage = body.get("query");
        var tags = mcpService.parseNaturalLanguage(naturalLanguage);
        // 拿到tags后再次调用search逻辑
        return Map.of("interpreted_tags", tags, "result", "Search logic here...");
    }
}