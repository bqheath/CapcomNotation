package com.appolonia.pinball;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ImageProcessor {
    Bitmap imageToProcess;
    Mat imageMat;

    public ImageProcessor (Bitmap imageToProcess){
        this.imageToProcess = imageToProcess;
    }

    public void processImageModifications(){
        imageMat = new Mat();
        Utils.bitmapToMat(imageToProcess, imageMat);
        Mat edges = new Mat(imageMat.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(imageMat, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(imageMat, edges, 80, 100);
        Utils.matToBitmap(imageMat, imageToProcess);

    }


}
