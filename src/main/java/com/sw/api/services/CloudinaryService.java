package com.sw.api.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        // Validación básica
        if (file.isEmpty()) {
            throw new IllegalArgumentException("No se enviaron archivos o está vacío.");
        }

        // Subida masiva al folder "spaceshift_inmuebles" para ordenarlo en Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "spaceshift_inmuebles"
        ));

        // Obtener la URL segura de retorno
        return uploadResult.get("secure_url").toString();
    }
}
