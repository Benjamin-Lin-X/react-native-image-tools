import {
  NativeModules,
} from 'react-native';

export default {
  createBinaryImage: (path, type, threshold, format, quality, frontColorString="000000ff", backColorString="ffffffff") => {
    if (format !== 'JPEG' && format !== 'PNG') {
      throw new Error('Only JPEG and PNG format are supported!');
    }

    return new Promise((resolve, reject) => {
      NativeModules.ImageTools.createBinaryImage(path, type, threshold, format, quality, frontColorString, backColorString, (err, response) => {
        if (err) {
          return reject(err);
        }

        resolve(response);
      });
    });
  },
  GetImageRGBAs: (path) => {
    return new Promise((resolve, reject) => {
      NativeModules.ImageTools.GetImageRGBAs(path, (err, response) => {
        if (err) {
          return reject(err);
        }

        resolve(response);
      });
    });
  },
};
