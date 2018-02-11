declare module "react-native-image-tools" {
    export interface Response {
        path: string;
        uri: string;
        base64: string;
        size?: number;
        name?: string;
    }

    export default class ImageTools {
        static createBinaryImage(
            uri: string, type: number, threshold: number,
            format: "PNG" | "JPEG" | "WEBP", quality: number,
            bOutputBase64: boolean,
            frontColorString: string, backColorString: string
        ): Promise<Response>;
        static GetImageRGBAs(
            uri: string
        ): Promise<Response>;
    }
}
