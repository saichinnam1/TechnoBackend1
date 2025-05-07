package com.ecommerce.ecommerce_backend;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.util.Map;

public class ClaudeBinaryUploader {

    public static void main(String[] args) {
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dvih7iqmk",
                "api_key", "-xyQBru6NeONvCYDASCGlikoos4",
                "api_secret", "nrvbG2lbQ3Y1ayIOFaY2hHjqrd0"
        ));

        try {
            File file = new File("path/to/your/image.jpg");
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "public_id", "products/1698123456789_image"
            ));
            System.out.println(uploadResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}