# React Native Image Tool

A React Native module that can process local images.

WARNING:
- Only support Android now
- iOS version will be released in Feb, 2018

## Function
- Get Image's RGBA data(array)
- Binary Image

## 功能 
- 获取图片的RGBA值（数组）
- 把图片转换成2值图

## 機能
- 画像のRGBAデータを取得（配列）
- 元画像から、2値化画像を生成

## Setup

Install the package:

* 😻 React Native >= 0.40
```
npm i --save react-native-image-tool
react-native link react-native-image-tool
```
If a java undefined error occured, add
```
compile project(':react-native-image-tool')
```
into your build.gradle

#### Manual linking
If your any reason you don want to link this project using 'react-native link', go to settings.gradle and add
```
include ':react-native-image-tool'
project(':react-native-image-tool').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-image-tool/android')
```
then go the file that you build the ReactInstance and add the packager to it.

```
  ReactInstanceManager.Builder builder = ReactInstanceManager.builder()
                .setApplication(application)
                .setDefaultHardwareBackBtnHandler(application.getGAMActivity())
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .setCurrentActivity((Activity) application.getGAMActivity())
                .addPackage(new ImageToolsPackage()) <------- (Add this package on the Builder list)
```

## Usage example

```javascript
import ImageTools from 'react-native-image-tool';

ImageTools.createBinaryImage(imageUri, type, threshold, compressFormat, quality, frontColorString="000000ff", backColorString="ffffffff").then((response) => {
  // response.uri is the URI of the new image that can now be displayed, uploaded...
  // response.path is the path of the new image
  // response.name is the name of the new image with the extension
  // response.size is the size of the new image
}).catch((err) => {
  // Oops, something went wrong. Check that the filename is correct and
  // inspect err to get more details.
});

ImageResizer.GetImageRGBAs(imageUri).then((response) => {
  // response.width is the URI of the new image that can now be displayed, uploaded...
  // response.height is the path of the new image
  // response.rgba is a array of image RGBA(R:bit0-7, G:bit8-15, B:bit16-23, A:bit24-32)
}).catch((err) => {
  // Oops, something went wrong. Check that the filename is correct and
  // inspect err to get more details.
});
```


## API

### `promise createBinaryImage(path, type, threshold, compressFormat, quality, frontColorString="000000ff", backColorString="ffffffff")`

The promise resolves with an object containing: `path`, `uri`, `name` and `size` of the new file. The URI can be used directly as the `source` of an [`<Image>`](https://facebook.github.io/react-native/docs/image.html) component.

Option | Description
------ | -----------
path | Path of image file, or a base64 encoded image string prefixed with 'data:image/imagetype' where `imagetype` is jpeg or png.
type | Only support 1 Now
threshold | 0-255
compressFormat | Can be either JPEG, PNG or WEBP (android only).
quality | A number between 0 and 100. Used for the JPEG compression.
frontColorString | A 32bit hex formatted string with RGBA(R:bit0-7, G:bit8-15, B:bit16-23, A:bit24-32).
backColorString | A 32bit hex formatted string with RGBA(R:bit0-7, G:bit8-15, B:bit16-23, A:bit24-32).

### `promise GetImageRGBAs(path)`

The promise resolves with an object containing: `width`, `height` and `rgba`. `rgba` is a array of RGBA data.

Option | Description
------ | -----------
path | Path of image file, or a base64 encoded image string prefixed with 'data:image/imagetype' where `imagetype` is jpeg or png.
