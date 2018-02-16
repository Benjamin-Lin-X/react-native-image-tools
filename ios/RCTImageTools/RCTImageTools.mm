//
//  ImageTools.mm
//
//  Created by Benjamin Lin on 2018/01/29.
//

#include "RCTImageTools.h"
#include "ImageHelpers.h"
#import <React/RCTImageLoader.h>


@implementation ImageTools

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE();

bool saveImage(NSString * fullPath, UIImage * image, NSString * format, float quality)
{
    NSData* data = nil;
    if ([format isEqualToString:@"JPEG"]) {
        data = UIImageJPEGRepresentation(image, quality / 100.0);
    } else if ([format isEqualToString:@"PNG"]) {
        data = UIImagePNGRepresentation(image);
    }
    
    if (data == nil) {
        return NO;
    }
    
    NSFileManager* fileManager = [NSFileManager defaultManager];
    return [fileManager createFileAtPath:fullPath contents:data attributes:nil];
}

NSString * generateFilePath(NSString * ext, NSString * outputPath)
{
    NSString* directory;

    if ([outputPath length] == 0) {
        NSArray* paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
        directory = [paths firstObject];
    } else {
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDirectory = [paths objectAtIndex:0];
        directory = [documentsDirectory stringByAppendingPathComponent:outputPath];
        NSError *error;
        [[NSFileManager defaultManager] createDirectoryAtPath:directory withIntermediateDirectories:YES attributes:nil error:&error];
        if (error) {
            NSLog(@"Error creating documents subdirectory: %@", error);
            @throw [NSException exceptionWithName:@"InvalidPathException" reason:[NSString stringWithFormat:@"Error creating documents subdirectory: %@", error] userInfo:nil];
        }
    }

    NSString* name = [[NSUUID UUID] UUIDString];
    NSString* fullName = [NSString stringWithFormat:@"%@.%@", name, ext];
    NSString* fullPath = [directory stringByAppendingPathComponent:fullName];

    return fullPath;
}

typedef unsigned char byte;

UIImage * bmp2Binary(UIImage * sourceImage, int threshold, UInt32 frontColor, UInt32 backColor)
{
    CGContextRef ctx;
    CGImageRef imageRef = [sourceImage CGImage];
    NSUInteger width = CGImageGetWidth(imageRef);
    NSUInteger height = CGImageGetHeight(imageRef);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    byte *rawData = (byte*)malloc(height * width * 4);
    NSUInteger bytesPerPixel = 4;
    NSUInteger bytesPerRow = bytesPerPixel * width;
    NSUInteger bitsPerComponent = 8;
    CGContextRef context = CGBitmapContextCreate(rawData, width, height,
                                                 bitsPerComponent, bytesPerRow, colorSpace,
                                                 kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    
    CGContextDrawImage(context, CGRectMake(0, 0, width, height), imageRef);
    CGContextRelease(context);

    int byteIndex = 0;
    for (int ii = 0 ; ii < width * height ; ++ii)
    {
        int grey = (rawData[byteIndex] + rawData[byteIndex+1] + rawData[byteIndex+2]);

        UInt32* pixelPtr = (UInt32*)(rawData + byteIndex);

        if ( grey >= threshold * 3) {
            pixelPtr[0] = backColor;
        }
        else {
            pixelPtr[0] = frontColor;
        }
        
        byteIndex += 4;
    }
    
    ctx = CGBitmapContextCreate(rawData,
                                CGImageGetWidth( imageRef ),
                                CGImageGetHeight( imageRef ),
                                8,
                                bytesPerRow,
                                colorSpace,
                                kCGImageAlphaPremultipliedLast ); 
    CGColorSpaceRelease(colorSpace);
    
    imageRef = CGBitmapContextCreateImage (ctx);
    UIImage* rawImage = [UIImage imageWithCGImage:imageRef];  
    CGImageRelease(imageRef);
    
    CGContextRelease(ctx);  
    free(rawData);

    return rawImage;
}

RCT_EXPORT_METHOD(createBinaryImage:(NSString *)path
                  type:(int)type
                  threshold:(int)threshold
                  format:(NSString *)format
                  quality:(float)quality
                  bOutputBase64: (BOOL)bOutputBase64
                  frontColorString:(NSString *)frontColorString
                  backColorString:(NSString *)backColorString
                  callback:(RCTResponseSenderBlock)callback)
{
    
    //Set image extension
    NSString *extension = @"jpg";
    if ([format isEqualToString:@"PNG"]) {
        extension = @"png";
    }

    
    NSString* fullPath;
    @try {
        fullPath = generateFilePath(extension, @"");
    } @catch (NSException *exception) {
        callback(@[@"Invalid output path.", @""]);
        return;
    }

    [_bridge.imageLoader loadImageWithURLRequest:[RCTConvert NSURLRequest:path] callback:^(NSError *error, UIImage *image) {
        if (error || image == nil) {
            if ([path hasPrefix:@"data:"] || [path hasPrefix:@"file:"]) {
                NSURL *imageUrl = [[NSURL alloc] initWithString:path];
                image = [UIImage imageWithData:[NSData dataWithContentsOfURL:imageUrl]];
            } else {
                image = [[UIImage alloc] initWithContentsOfFile:path];
            }
            if (image == nil) {
                callback(@[@"Can't retrieve the file from the path.", @""]);
                return;
            }
        }

        UInt32 frontColor = 0;
        [[NSScanner scannerWithString:frontColorString] scanHexInt:&frontColor];
        UInt32 backColor = 65535;
        [[NSScanner scannerWithString:backColorString] scanHexInt:&backColor];
        switch (type) {
            case 1:
                image = bmp2Binary(image, threshold, frontColor, backColor);
                break;
            default:
                break;
        }

        if (bOutputBase64) {
            NSData * dataImage = UIImagePNGRepresentation(image);
            NSString * base64 = [dataImage base64EncodedStringWithOptions:0];

            NSDictionary *response = @{@"base64": base64};
            
            callback(@[[NSNull null], response]);
        }
        else {
            // Compress and save the image
            if (!saveImage(fullPath, image, format, quality)) {
                callback(@[@"Can't save the image. Check your compression format and your output path", @""]);
                return;
            }
            NSURL *fileUrl = [[NSURL alloc] initFileURLWithPath:fullPath];
            NSString *fileName = fileUrl.lastPathComponent;
            NSError *attributesError = nil;
            NSDictionary *fileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:fullPath error:&attributesError];
            NSNumber *fileSize = fileAttributes == nil ? 0 : [fileAttributes objectForKey:NSFileSize];
            NSDictionary *response = @{@"path": fullPath,
                                    @"uri": fileUrl.absoluteString,
                                    @"name": fileName,
                                    @"size": fileSize == nil ? @(0) : fileSize
                                    };
            
            callback(@[[NSNull null], response]);
        }
    }];
}

RCT_EXPORT_METHOD(GetImageRGBAs:(NSString *)path
                  callback:(RCTResponseSenderBlock)callback)
{
    [_bridge.imageLoader loadImageWithURLRequest:[RCTConvert NSURLRequest:path] callback:^(NSError *error, UIImage *image) {
        if (error || image == nil) {
            if ([path hasPrefix:@"data:"] || [path hasPrefix:@"file:"]) {
                NSURL *imageUrl = [[NSURL alloc] initWithString:path];
                image = [UIImage imageWithData:[NSData dataWithContentsOfURL:imageUrl]];
            } else {
                image = [[UIImage alloc] initWithContentsOfFile:path];
            }
            if (image == nil) {
                callback(@[@"Can't retrieve the file from the path.", @""]);
                return;
            }
        }

        CGImageRef imageRef = [image CGImage];
        NSUInteger width = CGImageGetWidth(imageRef);
        NSUInteger height = CGImageGetHeight(imageRef);
        CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
        byte *rawData = (byte*)malloc(height * width * 4);
        NSUInteger bytesPerPixel = 4;
        NSUInteger bytesPerRow = bytesPerPixel * width;
        NSUInteger bitsPerComponent = 8;
        CGContextRef context = CGBitmapContextCreate(rawData, width, height,
                                                    bitsPerComponent, bytesPerRow, colorSpace,
                                                    kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
        
        CGContextDrawImage(context, CGRectMake(0, 0, width, height), imageRef);
        CGContextRelease(context);

        NSMutableArray *pixelArray = [NSMutableArray array];

        int byteIndex = 0;
        for (int ii = 0 ; ii < width * height ; ++ii)
        {
            int grey = (rawData[byteIndex] + rawData[byteIndex+1] + rawData[byteIndex+2]);

            UInt32* pixelPtr = (UInt32*)(rawData + byteIndex);

            UInt32 rgba = *(pixelPtr);
            NSNumber * rgbaNumber = [NSNumber numberWithInt:rgba];
            [pixelArray addObject:rgbaNumber];
            
            byteIndex += 4;
        }
        
        CGColorSpaceRelease(colorSpace);
        free(rawData);

        NSNumber * imageWidth = @(image.size.width);
        NSNumber * imageHeight = @(image.size.height);
        NSDictionary *response = @{@"width": imageWidth,
                                   @"height": imageHeight,
                                   @"rgba": pixelArray
                                   };
        
        callback(@[[NSNull null], response]);
    }];
}
@end
