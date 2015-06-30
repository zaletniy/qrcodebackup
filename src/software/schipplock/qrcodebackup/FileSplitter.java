package software.schipplock.qrcodebackup;

import java.io.*;
import java.util.ArrayList;

public class FileSplitter {
    private File file;
    private int lastChunkSize = 0;

    private ArrayList<Integer> chunkSizes = new ArrayList<Integer>();

    FileSplitter(File file) {
        this.file = file;
    }

    private File getFile() {
        return this.file;
    }

    public int getChunkSize(int index) {
        return this.chunkSizes.get(index);
    }

    public String[] split(int chunkSizeInByte) throws IOException {
        ArrayList<String> chunks = new ArrayList<String>();
        File file = this.getFile();
        float fileSize = file.length();
        int numberOfChunks = (int)Math.ceil(fileSize / chunkSizeInByte);

        BufferedInputStream f = new BufferedInputStream(new FileInputStream(file));

        for (int run = 0; run < numberOfChunks; run++) {
            byte[] buffer = new byte[chunkSizeInByte];
            this.chunkSizes.add(f.read(buffer, 0, chunkSizeInByte));
            chunks.add(new String(buffer));
        }

        String[] parts = new String[chunks.size()];
        parts = chunks.toArray(parts);

        return parts;
    }
}
