package com.example.androidthings.imageclassifier;

public class ExternalStorageHelper {
    private static ExternalStorageHelper instance = null;

    private ExternalStorageHelper(){}

    public static ExternalStorageHelper getInstance()
    {
        if (instance == null) {
            instance = new ExternalStorageHelper();
        }

        return instance;
    }

}
