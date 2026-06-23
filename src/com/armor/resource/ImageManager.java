package com.armor.resource;

import java.awt.*;
import java.util.HashMap;// 用來管理圖片
import java.util.Map;
import javax.swing.ImageIcon;

public class ImageManager {
    private Map<String, Image> images = new HashMap<>();// 儲存所有圖片

    public ImageManager() {
        loadAllImages();
    }

    private void loadAllImages() {
        loadAsset("idle");
        loadAsset("guard");
        loadAsset("H_guard");
        loadAsset("L_atk");
        loadAsset("M_atk");
        loadAsset("H_atk");
        loadAsset("H_ready");
        loadAsset("get_hit");
        loadAsset("parry");
        loadAsset("guard_break");
        loadAsset("win");
        loadAsset("lose");
    }

    // 此函式用來載入圖片
    private void loadAsset(String name) {
        String resourcePath = "image/" + name + ".png";
        Image image = loadImageFromClasspath(resourcePath);

        if (image == null) {
            image = loadImageFromFile(resourcePath);
        }

        if (image != null) {
            images.put(name, image);
            System.out.println("載入成功: " + name + " (" + getLastLoadedPath(name) + ")");
        } else {
            System.err.println("載入失敗 (找不到檔案): " + resourcePath + " | cwd=" + System.getProperty("user.dir"));
        }
    }

    private Image loadImageFromClasspath(String resourcePath) {
        java.net.URL url = getClass().getClassLoader().getResource(resourcePath);
        if (url != null) {
            lastLoadedPath = resourcePath + " [classpath]";
            return new ImageIcon(url).getImage();
        }
        return null;
    }

    private Image loadImageFromFile(String relativePath) {
        Image image = loadImageFromFilePath(relativePath);
        if (image != null) {
            return image;
        }

        Image altImage = loadImageFromFilePath("ProjectArmor/" + relativePath);
        if (altImage != null) {
            return altImage;
        }

        Image imageFromCodeBase = loadImageFromCodeBase(relativePath);
        if (imageFromCodeBase != null) {
            return imageFromCodeBase;
        }

        return null;
    }

    private Image loadImageFromFilePath(String path) {
        ImageIcon icon = new ImageIcon(path);
        if (icon.getIconWidth() != -1) {
            lastLoadedPath = new java.io.File(path).getAbsolutePath();
            return icon.getImage();
        }
        return null;
    }

    private Image loadImageFromCodeBase(String relativePath) {
        try {
            java.io.File codeSource = new java.io.File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            java.io.File baseDir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();
            // bin/../image 或 jar/../image
            java.io.File imageFile = new java.io.File(baseDir.getParentFile(), relativePath);
            if (imageFile.exists()) {
                ImageIcon icon = new ImageIcon(imageFile.getAbsolutePath());
                if (icon.getIconWidth() != -1) {
                    lastLoadedPath = imageFile.getAbsolutePath();
                    return icon.getImage();
                }
            }
        } catch (Exception e) {
            // 忽略，這只是嘗試用 code source 位置尋找資源
        }
        return null;
    }

    private String getLastLoadedPath(String name) {
        return lastLoadedPath == null ? "unknown" : lastLoadedPath;
    }

    private String lastLoadedPath;

    // 讓外部存取圖片
    public Image getImage(String name) {
        return images.get(name);
    }

}
