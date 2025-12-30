package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.response.FaceRecognitionResponse;
import com.wholesaler.inventory.entity.Customer;
import com.wholesaler.inventory.entity.CustomerFaceImage;
import com.wholesaler.inventory.exception.BusinessException;
import com.wholesaler.inventory.repository.CustomerFaceImageRepository;
import com.wholesaler.inventory.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FaceRecognitionService {

    private final CustomerRepository customerRepository;
    private final CustomerFaceImageRepository faceImageRepository;
    private final CascadeClassifier faceCascade;

    @Value("${app.face-recognition.similarity-threshold:0.6}")
    private double similarityThreshold;

    @Value("${app.face-recognition.max-face-images-per-customer:5}")
    private int maxFaceImagesPerCustomer;

    @Value("${app.face-recognition.async-processing:true}")
    private boolean asyncProcessing;

    private static final String UPLOAD_DIR = "uploads/face-images/";
    private static final int IMG_SIZE = 200;

    public FaceRecognitionService(CustomerRepository customerRepository,
                                  CustomerFaceImageRepository faceImageRepository) {
        this.customerRepository = customerRepository;
        this.faceImageRepository = faceImageRepository;

        // Load OpenCV
        try {
            OpenCV.loadLocally();
            log.info("OpenCV loaded successfully");
        } catch (Exception e) {
            log.error("Failed to load OpenCV", e);
            throw new RuntimeException("Failed to initialize OpenCV", e);
        }

        // Initialize face cascade classifier
        faceCascade = new CascadeClassifier();

        // Try to load Haar Cascade classifier from classpath or system
        try {
            String cascadePath = getClass().getClassLoader()
                    .getResource("haarcascade_frontalface_default.xml")
                    .getPath();

            if (!faceCascade.load(cascadePath)) {
                // Fallback to OpenCV data directory
                cascadePath = "/usr/share/opencv4/haarcascades/haarcascade_frontalface_default.xml";
                if (!faceCascade.load(cascadePath)) {
                    log.warn("Could not load face cascade classifier");
                }
            } else {
                log.info("Face cascade classifier loaded successfully");
            }
        } catch (Exception e) {
            log.warn("Face cascade classifier not loaded, face detection may not work", e);
        }

        // Create upload directory
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            log.error("Failed to create upload directory", e);
        }
    }

    @Transactional
    public CustomerFaceImage saveFaceImage(Long customerId, MultipartFile imageFile) throws IOException {
        log.info("Saving face image for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException("Customer not found: " + customerId));

        // Check max images limit
        long existingCount = faceImageRepository.countByCustomerId(customerId);
        if (existingCount >= maxFaceImagesPerCustomer) {
            throw new BusinessException(
                    String.format("Maximum %d face images already stored for this customer", maxFaceImagesPerCustomer));
        }

        // Save image file
        String filename = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + filename);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create face image entity
        CustomerFaceImage faceImage = CustomerFaceImage.builder()
                .customer(customer)
                .imagePath(filePath.toString())
                .encodingStatus("PENDING")
                .isPrimary(existingCount == 0)
                .build();

        faceImage = faceImageRepository.save(faceImage);

        // Process face encoding asynchronously
        if (asyncProcessing) {
            processFaceEncodingAsync(faceImage.getId());
        } else {
            processFaceEncoding(faceImage.getId());
        }

        return faceImage;
    }

    @Async
    public CompletableFuture<Void> processFaceEncodingAsync(Long faceImageId) {
        processFaceEncoding(faceImageId);
        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void processFaceEncoding(Long faceImageId) {
        log.info("Processing face encoding for image: {}", faceImageId);

        CustomerFaceImage faceImage = faceImageRepository.findById(faceImageId)
                .orElseThrow(() -> new BusinessException("Face image not found"));

        try {
            // Load image
            Mat image = Imgcodecs.imread(faceImage.getImagePath());
            if (image.empty()) {
                throw new BusinessException("Could not load image from path: " + faceImage.getImagePath());
            }

            // Detect face
            MatOfRect faceDetections = new MatOfRect();
            faceCascade.detectMultiScale(image, faceDetections);

            if (faceDetections.toArray().length == 0) {
                faceImage.setEncodingStatus("FAILED");
                faceImageRepository.save(faceImage);
                log.warn("No face detected in image: {}", faceImageId);
                return;
            }

            // Get first detected face
            Rect faceRect = faceDetections.toArray()[0];
            Mat faceROI = new Mat(image, faceRect);

            // Resize to standard size
            Mat resizedFace = new Mat();
            Imgproc.resize(faceROI, resizedFace, new Size(IMG_SIZE, IMG_SIZE));

            // Convert to grayscale
            Mat grayFace = new Mat();
            Imgproc.cvtColor(resizedFace, grayFace, Imgproc.COLOR_BGR2GRAY);

            // Flatten to 1D array for storage
            byte[] faceData = new byte[(int) (grayFace.total() * grayFace.elemSize())];
            grayFace.get(0, 0, faceData);

            // Save embedding
            faceImage.setFaceEmbedding(faceData);
            faceImage.setEncodingStatus("COMPLETED");
            faceImageRepository.save(faceImage);

            log.info("Face encoding completed successfully for image: {}", faceImageId);

        } catch (Exception e) {
            log.error("Failed to process face encoding", e);
            faceImage.setEncodingStatus("FAILED");
            faceImageRepository.save(faceImage);
            throw new BusinessException("Failed to process face image: " + e.getMessage());
        }
    }

    public FaceRecognitionResponse recognizeFace(MultipartFile imageFile) throws IOException {
        log.info("Recognizing face from uploaded image");

        // Save temp file
        String tempFilename = "temp_" + UUID.randomUUID().toString() + ".jpg";
        Path tempPath = Paths.get(UPLOAD_DIR + tempFilename);
        Files.copy(imageFile.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);

        try {
            // Load and process input image
            Mat inputImage = Imgcodecs.imread(tempPath.toString());
            if (inputImage.empty()) {
                throw new BusinessException("Could not load uploaded image");
            }

            // Detect face
            MatOfRect faceDetections = new MatOfRect();
            faceCascade.detectMultiScale(inputImage, faceDetections);

            if (faceDetections.toArray().length == 0) {
                return FaceRecognitionResponse.builder()
                        .recognized(false)
                        .message("No face detected in the image")
                        .build();
            }

            // Get first detected face
            Rect faceRect = faceDetections.toArray()[0];
            Mat faceROI = new Mat(inputImage, faceRect);

            // Resize and convert to grayscale
            Mat resizedFace = new Mat();
            Imgproc.resize(faceROI, resizedFace, new Size(IMG_SIZE, IMG_SIZE));

            Mat grayFace = new Mat();
            Imgproc.cvtColor(resizedFace, grayFace, Imgproc.COLOR_BGR2GRAY);

            // Get face data
            byte[] inputFaceData = new byte[(int) (grayFace.total() * grayFace.elemSize())];
            grayFace.get(0, 0, inputFaceData);

            // Match against all stored faces
            List<CustomerFaceImage> allFaces = faceImageRepository.findByEncodingStatus("COMPLETED");

            double bestScore = 0.0;
            Customer bestMatchCustomer = null;

            for (CustomerFaceImage storedFace : allFaces) {
                double similarity = calculateSimilarity(inputFaceData, storedFace.getFaceEmbedding());

                if (similarity > bestScore) {
                    bestScore = similarity;
                    bestMatchCustomer = storedFace.getCustomer();
                }
            }

            // Check if best match exceeds threshold
            if (bestScore >= similarityThreshold && bestMatchCustomer != null) {
                return FaceRecognitionResponse.builder()
                        .recognized(true)
                        .customerId(bestMatchCustomer.getId())
                        .customerName(bestMatchCustomer.getCustomerName())
                        .phoneNumber(bestMatchCustomer.getPhoneNumber())
                        .confidenceScore(bestScore)
                        .message("Customer recognized successfully")
                        .build();
            } else {
                return FaceRecognitionResponse.builder()
                        .recognized(false)
                        .confidenceScore(bestScore)
                        .message("No matching customer found")
                        .build();
            }

        } finally {
            // Clean up temp file
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempPath);
            }
        }
    }

    private double calculateSimilarity(byte[] face1, byte[] face2) {
        if (face1 == null || face2 == null || face1.length != face2.length) {
            return 0.0;
        }

        // Calculate normalized correlation (simple similarity metric)
        double sum = 0.0;
        double sum1Sq = 0.0;
        double sum2Sq = 0.0;

        for (int i = 0; i < face1.length; i++) {
            int val1 = face1[i] & 0xFF;
            int val2 = face2[i] & 0xFF;

            sum += val1 * val2;
            sum1Sq += val1 * val1;
            sum2Sq += val2 * val2;
        }

        double denominator = Math.sqrt(sum1Sq * sum2Sq);
        if (denominator == 0) {
            return 0.0;
        }

        // Return normalized correlation coefficient (0 to 1)
        return sum / denominator;
    }

    @Transactional(readOnly = true)
    public List<CustomerFaceImage> getCustomerFaceImages(Long customerId) {
        return faceImageRepository.findByCustomerId(customerId);
    }

    @Transactional
    public void deleteFaceImage(Long faceImageId) {
        CustomerFaceImage faceImage = faceImageRepository.findById(faceImageId)
                .orElseThrow(() -> new BusinessException("Face image not found"));

        // Delete physical file
        try {
            Files.deleteIfExists(Paths.get(faceImage.getImagePath()));
        } catch (IOException e) {
            log.warn("Failed to delete face image file: {}", faceImage.getImagePath());
        }

        // Delete from database
        faceImageRepository.delete(faceImage);
        log.info("Face image deleted: {}", faceImageId);
    }
}
