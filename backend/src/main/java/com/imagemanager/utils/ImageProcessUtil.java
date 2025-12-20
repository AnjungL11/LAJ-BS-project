package com.imagemanager.utils;
import com.imagemanager.entity.ImageMetadata;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.imaging.Imaging;
// import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
// import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImageProcessUtil {

    // 生成缩略图
    public static void createThumbnail(File original, File thumbnail) throws IOException {
        Thumbnails.of(original)
                .size(300, 300) // 固定尺寸
                .outputQuality(0.8)
                .toFile(thumbnail);
    }

    // EXIF日期格式通常为 "yyyy:MM:dd HH:mm:ss"
    private static final DateTimeFormatter EXIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    // 提取EXIF信息
    public static ImageMetadata extractMetadata(File file) {
        ImageMetadata meta = new ImageMetadata();
        try {
            // 获取 EXIF 元数据
            // org.apache.commons.imaging.common.ImageMetadata commonMeta = Imaging.getMetadata(file);
            var commonMeta = Imaging.getMetadata(file);
            if (commonMeta instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMeta = (JpegImageMetadata) commonMeta;
                TiffImageMetadata exif = jpegMeta.getExif();
                
                if (exif != null) {
                    // 获取相机型号
                    // String model = exif.getFieldValue(ExifTagConstants.EXIF_TAG_MODEL)[0];
                    // 使用 TiffTagConstants
                    // String model = exif.getFieldValue(TiffTagConstants.TIFF_TAG_MODEL)[0];
                    // meta.setCameraModel(model);
                    String[] modelField = exif.getFieldValue(TiffTagConstants.TIFF_TAG_MODEL);
                    if (modelField != null && modelField.length > 0) {
                        meta.setCameraModel(modelField[0].trim());
                    }

                    // 获取拍摄时间
                    // meta.setTakenTime(...); 
                    String[] dateField = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                    if (dateField != null && dateField.length > 0) {
                        try {
                            // 解析时间格式
                            meta.setTakenTime(LocalDateTime.parse(dateField[0], EXIF_DATE_FORMAT));
                        } catch (Exception e) {
                            System.err.println("时间解析失败: " + dateField[0]);
                        }
                    }
                    
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