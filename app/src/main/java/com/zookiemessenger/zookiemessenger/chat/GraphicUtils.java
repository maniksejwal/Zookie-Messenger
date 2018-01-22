package com.zookiemessenger.zookiemessenger.chat;

import android.content.Context;
import android.widget.Toast;

import com.zookiemessenger.zookiemessenger.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by manik on 21/1/18.
 */

public class GraphicUtils {

    static File createTempImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalCacheDir();
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    static File createTempVideoFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "MP4_" + timeStamp + "_";
        File storageDir = context.getExternalCacheDir();
        return File.createTempFile(imageFileName, ".mp4", storageDir);
    }

    static boolean deleteGraphicFile(Context context, String imagePath) {
        // Get the file
        File imageFile = new File(imagePath);

        // Delete the image
        boolean deleted = imageFile.delete();

        // If there is an error deleting the file, show a Toast
        if (!deleted) {
            String errorMessage = context.getString(R.string.try_again);
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
        }

        return deleted;
    }
}
