package software.schipplock.qrcodebackup;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZ;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.*;

public class Compressor {

    public static Boolean compress(byte[] data, String targetFilename) throws IOException {
        FileOutputStream outfile = new FileOutputStream(targetFilename);
        XZOutputStream outxz = new XZOutputStream(outfile, new LZMA2Options(8), XZ.CHECK_SHA256);
        outxz.write(data);
        outxz.finish();
        return true;
    }

    public static Boolean decompress(String filename) throws IOException {
        InputStream infile = new FileInputStream(filename);
        XZInputStream inxz = new XZInputStream(infile);
        return true;
    }
}
