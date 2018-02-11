package com.benjamin.rnimagetools;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;

import java.io.File;
import java.io.IOException;

/**
 * Created by Benjamin Lin on 2018/01/29.
 */
public class ImageToolsModule extends ReactContextBaseJavaModule {
    private Context context;

    public ImageToolsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
    }

    /**
     * @return the name of this module. This will be the name used to {@code require()} this module
     * from javascript.
     */
    @Override
    public String getName() {
        return "ImageToolsAndroid";
    }

    @ReactMethod
    public void createBinaryImage(String imagePath, int type, int threshold, String compressFormat, int quality,
                            Boolean bOutputBase64, String strFrontColorRGBA, String strBackColorRGBA,
                            final Callback successCb, final Callback failureCb) {
        try {
            createBinaryImageWithExceptions(imagePath, type, threshold, compressFormat, quality,
                    bOutputBase64, strFrontColorRGBA, strBackColorRGBA,
                    successCb, failureCb);
        } catch (IOException e) {
            failureCb.invoke(e.getMessage());
        }
    }

    private void createBinaryImageWithExceptions(String imagePath, int type, int threshold,
                                           String compressFormatString, int quality, Boolean bOutputBase64,
                                           String strFrontColorRGBA, String strBackColorRGBA,
                                           final Callback successCb, final Callback failureCb) throws IOException {
        Bitmap.CompressFormat compressFormat = Bitmap.CompressFormat.valueOf(compressFormatString);
        Uri imageUri = Uri.parse(imagePath);

        Bitmap binaryImg = ImageTools.createBinaryImage(this.context, imageUri,
                type, threshold, compressFormat, quality, strFrontColorRGBA, strBackColorRGBA);

        WritableMap response = Arguments.createMap();
        if (bOutputBase64) {
            String base64 = ImageTools.bitmapToBase64(binaryImg);
            response.putString("base64", base64);
            binaryImg.recycle();
            // Invoke success
            successCb.invoke(response);
        }
        else {
            File fImage = ImageTools.SaveImg2File(this.context, binaryImg, compressFormat, quality);
            // If BinaryImagePath is empty and this wasn't caught earlier, throw.
            if (fImage.isFile()) {
                response.putString("path", fImage.getAbsolutePath());
                response.putString("uri", Uri.fromFile(fImage).toString());
                response.putString("name", fImage.getName());
                response.putDouble("size", fImage.length());
                // Invoke success
                successCb.invoke(response);
            } else {
                failureCb.invoke("Error getting Binary image path");
            }
        }
    }

    @ReactMethod
    public void GetImageRGBAs(String imagePath, final Callback successCb, final Callback failureCb) {
        try {
            GetImageRGBAsWithExceptions(imagePath, successCb, failureCb);
        } catch (IOException e) {
            failureCb.invoke(e.getMessage());
        }
    }

    private void GetImageRGBAsWithExceptions(String imagePath, final Callback successCb, final Callback failureCb) throws IOException {
        Uri imageUri = Uri.parse(imagePath);
        Bitmap sourceImage = ImageTools.LoadImage(this.context, imageUri);

        // If BinaryImagePath is empty and this wasn't caught earlier, throw.
        if (sourceImage != null) {
            WritableMap response = Arguments.createMap();
            response.putInt("width", sourceImage.getWidth());
            response.putInt("height", sourceImage.getHeight());

            WritableArray datas = Arguments.createArray();
            int[] RGBAs = ImageTools.GetImageRGBAs(sourceImage);
            for (int i = 0; i < RGBAs.length; i++) {
                datas.pushInt(RGBAs[i]);
            }
            response.putArray("rgba", datas);
            // Invoke success
            successCb.invoke(response);
        } else {
            failureCb.invoke("Error getting image RGBA");
        }
    }
}
