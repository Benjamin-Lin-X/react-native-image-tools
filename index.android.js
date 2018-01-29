import React from 'react-native';

const ImageToolsAndroid = React.NativeModules.ImageToolsAndroid;

export default {
  createBinaryImage: (imagePath, type, threshold, compressFormat, quality, frontColorString="000000ff", backColorString="ffffffff") => {
    return new Promise((resolve, reject) => {
      ImageToolsAndroid.createBinaryImage(imagePath, type, threshold, compressFormat, quality, frontColorString, backColorString,
        resolve, reject);
    });
  },
  GetImageRGBAs: (imagePath) => {
    return new Promise((resolve, reject) => {
      ImageToolsAndroid.GetImageRGBAs(imagePath,
        resolve, reject);
    });
  },
};
