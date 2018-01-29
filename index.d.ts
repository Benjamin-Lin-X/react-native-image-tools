declare module "react-native-image-tools" {
    export interface Response {
        path: string;
        uri: string;
        size?: number;
        name?: string;
    }

    export default class ImageTools {
        static createBinaryImage(
            uri: string, type: number, threshold: number,
            format: "PNG" | "JPEG" | "WEBP", quality: number,
            frontColorString: string, backColorString: string
        ): Promise<Response>;
        static GetImageRGBAs(
            uri: string
        ): Promise<Response>;
    }
}
