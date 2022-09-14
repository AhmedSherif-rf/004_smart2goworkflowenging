package com.ntg.common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * <p>Title: Network TeleCome System</p>
 *
 * <p>Description: NTS </p>
 *
 * <p>Copyright: Copyright (c) 2004, NTG-Clarity Network All rights reserved</p>
 *
 * <p>Company: NTG Clarity Egypt Office Co.</p>
 *
 * @author Ahmed Hashim
 * @version 3.0
 */
public class NTGCompressor {
  public NTGCompressor() {
  }

  public static boolean Compress(String FileName, String OutputFile,String FileCaption) {
    // Create a buffer for reading the files
    byte[] buf = new byte[1024];

    try {
      // Create the ZIP file
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
          OutputFile));

      // Compress the files
      FileInputStream in = new FileInputStream(FileName);

      // Add ZIP entry to output stream.
      out.putNextEntry(new ZipEntry(FileCaption));

      // Transfer bytes from the file to the ZIP file
      int len;
      while ( (len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }

      // Complete the entry
      out.closeEntry();
      in.close();
      out.close();
      return true;
    }
    catch (Exception ex) {
      NTGMessageOperation.PrintErrorTrace(ex);
    }
    return false;
  }
}
