package com.imagemanager.utils;
import com.imagemanager.entity.ImageMetadata;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;

public class ImageProcessUtil {

    // 生成缩略图 [cite: 92]
    public static void createThumbnail(File original, File thumbnail) throws IOException {
        Thumbnails.of(original)
                .size(300, 300) // 固定尺寸
                .outputQuality(0.8)
                .toFile(thumbnail);
    }

    // 提取EXIF信息 [cite: 89, 272]
    public static ImageMetadata extractMetadata(File file) {
        ImageMetadata meta = new ImageMetadata();
        try {
            org.apache.commons.imaging.common.ImageMetadata commonMeta = Imaging.getMetadata(file);
            if (commonMeta instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMeta = (JpegImageMetadata) commonMeta;
                TiffImageMetadata exif = jpegMeta.getExif();
                
                if (exif != null) {
                    // 获取相机型号
                    String model = exif.getFieldValue(ExifTagConstants.EXIF_TAG_MODEL)[0];
                    meta.setCameraModel(model);
                    // 获取拍摄时间 (这里简化处理，实际需解析字符串)
                    // meta.setTakenTime(...); 
                    
                    // 获取GPS (Apache Commons Imaging自带helper)
                    TiffImageMetadata.GPSInfo gps = exif.getGPS();
                    if (gps != null) {
                        meta.setGpsLatitude(gps.getLatitudeAsDegreesNorth());
                        meta.setGpsLongitude(gps.getLongitudeAsDegreesEast());
                    }
                }
            }
            // 获取宽高等基本信息
            var info = Imaging.getImageInfo(file);
            meta.setWidth(info.getWidth());
            meta.setHeight(info.getHeight());
        } catch (Exception e) {
            // 忽略EXIF解析错误，防止影响上传
            System.err.println("EXIF extraction failed: " + e.getMessage());
        }
        return meta;
    }
}