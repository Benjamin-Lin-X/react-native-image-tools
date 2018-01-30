package com.benjamin.rnimagetools;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;

/**
 * Provide methods to resize and rotate an image file.
 */
public class ImageTools {
    private final static String IMAGE_JPEG = "image/jpeg";
    private final static String IMAGE_PNG = "image/png";
    private final static String SCHEME_DATA = "data";
    private final static String SCHEME_CONTENT = "content";
    private final static String SCHEME_FILE = "file";

    private final static int MAX_AREA = 240*8*500*8;

    /**
     * Resize the specified bitmap, keeping its aspect ratio.
     */
    private static Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
        Bitmap newImage = null;
        if (image == null) {
            return null; // Can't load the image from the given path.
        }

        if (maxHeight > 0 && maxWidth > 0) {
            float width = image.getWidth();
            float height = image.getHeight();

            float ratio = Math.min((float)maxWidth / width, (float)maxHeight / height);

            int finalWidth = (int) (width * ratio);
            int finalHeight = (int) (height * ratio);
            try {
                newImage = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            } catch (OutOfMemoryError e) {
                return null;
            }
        }

        return newImage;
    }

    /**
     * Rotate the specified bitmap with the given angle, in degrees.
     */
    public static Bitmap rotateImage(Bitmap source, float angle)
    {
        Bitmap retVal;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            return null;
        }
        return retVal;
    }

    /**
     * Save the given bitmap in a directory. Extension is automatically generated using the bitmap format.
     */
    private static File saveImage(Bitmap bitmap, File saveDirectory, String fileName,
                                    Bitmap.CompressFormat compressFormat, int quality)
            throws IOException {
        if (bitmap == null) {
            throw new IOException("The bitmap couldn't be resized");
        }

        File newFile = new File(saveDirectory, fileName + "." + compressFormat.name());
        if(!newFile.createNewFile()) {
            throw new IOException("The file already exists");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(compressFormat, quality, outputStream);
        byte[] bitmapData = outputStream.toByteArray();

        outputStream.flush();
        outputStream.close();

        FileOutputStream fos = new FileOutputStream(newFile);
        fos.write(bitmapData);
        fos.flush();
        fos.close();

        return newFile;
    }

    /**
     * Get {@link File} object for the given Android URI.<br>
     * Use content resolver to get real path if direct path doesn't return valid file.
     */
    private static File getFileFromUri(Context context, Uri uri) {

        // first try by direct path
        File file = new File(uri.getPath());
        if (file.exists()) {
            return file;
        }

        // try reading real path from content resolver (gallery images)
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String realPath = cursor.getString(column_index);
            file = new File(realPath);
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return file;
    }

    /**
     * Compute the inSampleSize value to use to load a bitmap.
     */
    private static int calculateInSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int area = height * width;

        int inSampleSize = 1;
        while (area > MAX_AREA) {
            area = area / 4;
            inSampleSize *= 2;
        }

        return inSampleSize;
    }

    /**
     * Load a bitmap either from a real file or using the {@link ContentResolver} of the current
     * {@link Context} (to read gallery images for example).
     *
     * Note that, when options.inJustDecodeBounds = true, we actually expect sourceImage to remain
     * as null (see https://developer.android.com/training/displaying-bitmaps/load-bitmap.html), so
     * getting null sourceImage at the completion of this method is not always worthy of an error.
     */
    private static Bitmap loadBitmap(Context context, Uri imageUri, BitmapFactory.Options options) throws IOException {
        Bitmap sourceImage = null;
        String imageUriScheme = imageUri.getScheme();
        if (imageUriScheme == null || !imageUriScheme.equalsIgnoreCase(SCHEME_CONTENT)) {
            try {
                sourceImage = BitmapFactory.decodeFile(imageUri.getPath(), options);
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Error decoding image file");
            }
        } else {
            ContentResolver cr = context.getContentResolver();
            InputStream input = cr.openInputStream(imageUri);
            if (input != null) {
                sourceImage = BitmapFactory.decodeStream(input, null, options);
                input.close();
            }
        }
        return sourceImage;
    }

    /**
     * Loads the bitmap resource from the file specified in imagePath.
     */
    private static Bitmap loadBitmapFromFile(Context context, Uri imageUri) throws IOException  {
        // Decode the image bounds to find the size of the source image.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        loadBitmap(context, imageUri, options);

        // Set a sample size according to the image size to lower memory usage.
        options.inSampleSize = calculateInSampleSize(options);
        options.inJustDecodeBounds = false;
        //System.out.println(options.inSampleSize);
        return loadBitmap(context, imageUri, options);

    }

    /**
     * Loads the bitmap resource from a base64 encoded jpg or png.
     * Format is as such:
     * png: 'data:image/png;base64,iVBORw0KGgoAA...'
     * jpg: 'data:image/jpeg;base64,/9j/4AAQSkZJ...'
     */
    private static Bitmap loadBitmapFromBase64(Uri imageUri) {
        Bitmap sourceImage = null;
        String imagePath = imageUri.getSchemeSpecificPart();
        int commaLocation = imagePath.indexOf(',');
        if (commaLocation != -1) {
            final String mimeType = imagePath.substring(0, commaLocation).replace('\\','/').toLowerCase();
            final boolean isJpeg = mimeType.startsWith(IMAGE_JPEG);
            final boolean isPng = !isJpeg && mimeType.startsWith(IMAGE_PNG);

            if (isJpeg || isPng) {
                // base64 image. Convert to a bitmap.
                final String encodedImage = imagePath.substring(commaLocation + 1);
                final byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                sourceImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            }
        }

        return sourceImage;
    }

    private static Bitmap Bmp2Binary(Bitmap orgPic, int threshold, int frontColor, int backColor) {
        int width = orgPic.getWidth();
        int height = orgPic.getHeight();

        Bitmap binarymap = null;  
        binarymap = orgPic.copy(Bitmap.Config.ARGB_8888, true);  

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int col = binarymap.getPixel(i, j);

                int alpha = col & 0xFF000000;

                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);

                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);

                // New ARGB
                int newColor = 0;
                //对图像进行二值化处理
                if (gray <= threshold) {
                    newColor = frontColor;
                } else {
                    newColor = backColor;
                }
                //设置新图像的当前像素值
                binarymap.setPixel(i, j, newColor);
            }
        }
        return binarymap;
    }

    /**
     * Create a binary image.
     */
    public static File createBinaryImage(Context context, Uri imageUri, int type, int threshold,
                                            Bitmap.CompressFormat compressFormat, int quality,
                                            String strFrontColorRGBA, String strBackColorRGBA) throws IOException  {
        Bitmap sourceImage = ImageTools.LoadImage(context, imageUri);
        if (sourceImage == null) {
            throw new IOException("Unable to load source image from path");
        }

        int frontColorA = Integer.parseInt(strFrontColorRGBA.substring(0, 2), 16);
        int frontColorR = Integer.parseInt(strFrontColorRGBA.substring(2, 4), 16);
        int frontColorG = Integer.parseInt(strFrontColorRGBA.substring(4, 6), 16);
        int frontColorB = Integer.parseInt(strFrontColorRGBA.substring(6, 8), 16);
        int frontColor = (frontColorA << 24) + (frontColorR << 16) + (frontColorG << 8) + frontColorB;
        int backColorA = Integer.parseInt(strBackColorRGBA.substring(0, 2), 16);
        int backColorR = Integer.parseInt(strBackColorRGBA.substring(2, 4), 16);
        int backColorG = Integer.parseInt(strBackColorRGBA.substring(4, 6), 16);
        int backColorB = Integer.parseInt(strBackColorRGBA.substring(6, 8), 16);
        int backColor = (backColorA << 24) + (backColorR << 16) + (backColorG << 8) + backColorB;

        Bitmap binaryMap = null;
        switch (type) {
            case 1:
            binaryMap = ImageTools.Bmp2Binary(sourceImage, threshold, frontColor, backColor);
                break;
            default:
                throw new IOException("Unsupported type"+String.valueOf(type));
        }
        sourceImage.recycle();

        // Save the resulting image
        File path = context.getCacheDir();

        File newFile = ImageTools.saveImage(binaryMap, path,
                Long.toString(new Date().getTime()), compressFormat, quality);

        // Clean up remaining image
        binaryMap.recycle();

        return newFile;
    }

    /**
     * load image.
     */
    public static Bitmap LoadImage(Context context, Uri imageUri) throws IOException  {
        Bitmap sourceImage = null;
        String imageUriScheme = imageUri.getScheme();
        if (imageUriScheme == null || imageUriScheme.equalsIgnoreCase(SCHEME_FILE) || imageUriScheme.equalsIgnoreCase(SCHEME_CONTENT)) {
            sourceImage = ImageTools.loadBitmapFromFile(context, imageUri);
        } else if (imageUriScheme.equalsIgnoreCase(SCHEME_DATA)) {
            sourceImage = ImageTools.loadBitmapFromBase64(imageUri);
        }

        if (sourceImage == null) {
            throw new IOException("Unable to load source image from path");
        }

        return sourceImage;
    }

    /**
     * Get image's RGBA array.
     */
    public static int[] GetImageRGBAs(Bitmap sourceImage) throws IOException  {

        if (sourceImage != null) {
            int width = sourceImage.getWidth();
            int height = sourceImage.getHeight();
            int pixels[] = new int[width * height];
            sourceImage.getPixels(pixels, 0, width, 0, 0, width, height);

            return pixels;
        }
        return new int[1];
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }
}
