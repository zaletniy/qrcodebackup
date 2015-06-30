package software.schipplock.qrcodebackup.utils;

import java.io.*;
import java.nio.channels.*;

/**
 * @author andreass
 */
public class FileHelper {

    /**
     * copies file "in" to file "out"
     *
     * @param in
     * @param out
     * @throws java.io.IOException
     */
    public static boolean copy(File in, File out) {
        Boolean returnVal = true;
        try {
            FileChannel inChannel = new
                    FileInputStream(in).getChannel();
            FileChannel outChannel = new
                    FileOutputStream(out).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(),
                        outChannel);
            } catch (IOException e) {
                returnVal = false;
            } finally {
                if (inChannel != null) inChannel.close();
                if (outChannel != null) outChannel.close();
            }
        } catch (Exception ea) {
            returnVal = false;
        }
        return returnVal;
    }

    /**
     * deletes a given filename
     *
     * @param fileName
     * @return
     */
    public static boolean delete(String fileName) {
        boolean returnVal = true;
        try {
            File target = new File(fileName);

            if (!target.exists()) {
                returnVal = false;
            }

            if (target.delete()) {
                returnVal = true;
            } else {
                returnVal = false;
            }
        } catch (SecurityException e) {
            returnVal = false;
        }

        return returnVal;
    }

    /**
     * lists files for a given directory
     *
     * @param directory
     * @return
     */
    public static String[] listFiles(String directory) {
        File dir = new File(directory);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return !name.startsWith(".");
            }
        };
        String[] children = dir.list(filter);
        return children;
    }

    /**
     * checks if a file exists
     *
     * @param fileName
     * @return
     */
    public static Boolean fileExists(String fileName) {
        File f = new File(fileName);
        return f.exists();
    }

    /**
     * checks if a specific directory is readable and writable
     *
     * @param directoryName
     * @return
     */
    public static Boolean isDirectoryValid(String directoryName) {
        File dir = new File(directoryName);
        Boolean returnVal = false;
        if (dir.canRead() && dir.canWrite()) {
            returnVal = true;
        } else {
        }
        return returnVal;
    }

    /**
     * the same as isDirectoryValid (only for a clear name!)
     *
     * @param FileName
     * @return
     */
    public static Boolean isFileWritable(String FileName) {
        return FileHelper.isDirectoryValid(FileName);
    }

    /**
     * php equivalent to file_put_contents
     *
     * @param filename
     * @param content
     */
    public static void filePutContents(String filename, String content) {
        File outFile = new File(filename);
        try {
            FileWriter out = new FileWriter(outFile);
            out.write(content);
            out.close();
        } catch (Exception e) {
            System.out.println("error writing " + filename);
        }
    }

    /**
     * @param filename
     * @return
     */
    public static String fileGetContents(String filename) throws IOException {
        byte[] buffer = new byte[(int) new File(filename).length()];
        BufferedInputStream f = new BufferedInputStream(new FileInputStream(filename));
        f.read(buffer);
        return new String(buffer);
    }

    /**
     * return file content as byte[]
     * @param filename
     * @return
     * @throws IOException
     */
    public static byte[] fileGetBytes(String filename) throws IOException {
        byte[] buffer = new byte[(int) new File(filename).length()];
        BufferedInputStream f = new BufferedInputStream(new FileInputStream(filename));
        f.read(buffer);
        return buffer;
    }

    public static String streamToString(InputStream is) throws IOException {
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    public static String root() throws IOException {
        return new File(".").getCanonicalPath();
    }

    public static String basename(String filename) {
        return new File(filename).getName();
    }

}
