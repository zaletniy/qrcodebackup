package software.schipplock.qrcodebackup;

import com.google.zxing.*;
import com.google.zxing.Reader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDPixelMap;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.imgscalr.Scalr;
import software.schipplock.qrcodebackup.utils.FileHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import org.apache.commons.codec.binary.Base64;

public class QrCodeHelper {
    private int maxByte = 2331;
    private String filename;

    private static final Map<DecodeHintType,Object> HINTS;
    private static final Map<DecodeHintType,Object> HINTS_PURE;

    static {
        HINTS = new EnumMap<>(DecodeHintType.class);
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
        HINTS_PURE = new EnumMap<>(HINTS);
        HINTS_PURE.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }

    public QrCodeHelper(String filename) {
        this.filename = filename;
    }

    private int getMaxByte() {
        return this.maxByte;
    }

    private String getFilename() {
        return this.filename;
    }

    public Boolean generate(String targetDirectory) throws IOException, WriterException, COSVisitorException {
        Boolean returnValue = true;
        String basename = FileHelper.basename(this.getFilename());
        String sourceFile = this.getFilename();
        String targetFile = targetDirectory + "/" + basename;

        // first compress the input file
        Compressor.compress(FileHelper.fileGetBytes(sourceFile), targetFile + ".xz");

        // create a base64 version of this file
        Base64 encoder = new Base64();
        FileHelper.filePutContents(targetFile + ".xz.base64", encoder.encodeAsString(Files.readAllBytes(Paths.get(targetFile + ".xz"))));

        // now split the compressed and base64 encoded file if needed
        FileSplitter fileSplitter = new FileSplitter(new File(targetFile + ".xz.base64"));
        String[] parts = fileSplitter.split(this.getMaxByte());
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        for (int run = 0; run < parts.length; run++) {
            String filePath = targetFile + "_" + Integer.toString(run+1) + "_" + parts.length + ".png";
            String fileType = "png";
            File myFile = new File(filePath);

            BitMatrix byteMatrix = qrCodeWriter.encode(parts[run], BarcodeFormat.QR_CODE, 800, 800, hintMap);
            int CrunchifyWidth = byteMatrix.getWidth();

            BufferedImage image = new BufferedImage(CrunchifyWidth, CrunchifyWidth, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();
            Graphics2D graphics = (Graphics2D) image.getGraphics();

            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, CrunchifyWidth, CrunchifyWidth);
            graphics.setColor(Color.BLACK);

            for (int i = 0; i < CrunchifyWidth; i++) {
                for (int j = 0; j < CrunchifyWidth; j++) {
                    if (byteMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }
            returnValue = ImageIO.write(image, fileType, myFile);

            if (returnValue) {
                // create pdf so we can easily print it
                PDDocument document = new PDDocument();
                PDPage page = new PDPage();
                page.setMediaBox(new PDRectangle(259.63782F, 418.52756F));
                document.addPage(page);

                PDFont font = PDType1Font.COURIER;

                PDPageContentStream contentStream = new PDPageContentStream(document, page);

                // add _the_ image
                PDXObjectImage ximage = new PDPixelMap(document, image);
                float scale = 0.357f;
                contentStream.drawXObject(ximage, -11, 135, ximage.getWidth() * scale, ximage.getHeight() * scale);

                // add some info text; this is actually quite important when you have more than one qr code for one file
                contentStream.beginText();
                contentStream.setFont(font, 8);
                contentStream.moveTextPositionByAmount(5, 135);
                contentStream.drawString("Autor  : " + "Andreas Schipplock");
                contentStream.moveTextPositionByAmount(0, -9);
                contentStream.drawString("Datei  : " + FileHelper.basename(filePath.replace(".png", "").split("_")[1]));
                contentStream.moveTextPositionByAmount(0, -9);
                contentStream.drawString("Format : " + "xz 1.0.4, base64");
                contentStream.moveTextPositionByAmount(0, -9);
                contentStream.drawString("Datum  : " + LocalDate.now());
                contentStream.moveTextPositionByAmount(0, -9);
                contentStream.drawString("Größe  : " + fileSplitter.getChunkSize(run) + " Bytes");
                contentStream.moveTextPositionByAmount(0, -9);
                contentStream.drawString("Nummer : " + FileHelper.basename(filePath.replace(".png", "").split("_")[2]));
                contentStream.moveTextPositionByAmount(0, -9);
                contentStream.drawString("Anzahl : " + FileHelper.basename(filePath.replace(".png", "").split("_")[3]));
                contentStream.endText();

                contentStream.close();

                document.save(filePath.replace(".png", ".pdf"));
                document.close();
            }
        }

        return returnValue;
    }

    public Boolean decode(String targetDirectory) throws IOException, NotFoundException {
        ArrayList<Result> results = new ArrayList<Result>();
        ReaderException savedException = null;
        ArrayList<String> contents = new ArrayList<String>();
        FileInputStream imageFile = new FileInputStream(this.getFilename());
        LuminanceSource source = new BufferedImageLuminanceSource(ImageIO.read(imageFile));
        BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
        Reader reader = new MultiFormatReader();
        Base64 decoder = new Base64();

        try {
            MultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(reader);
            Result[] theResults = multiReader.decodeMultiple(bitmap, HINTS);

            for(int run = 0; run < theResults.length; run++) {
                results.add(theResults[run]);
            }
        } catch (ReaderException re) {
            savedException = re;
        }

        if (results.isEmpty()) {
            try {
                // Look for pure barcode
                Result theResult = reader.decode(bitmap, HINTS_PURE);
                if (theResult != null) {
                    results.add(theResult);
                }
            } catch (ReaderException re) {
                savedException = re;
            }
        }

        if (results.isEmpty()) {
            try {
                // Look for normal barcode in photo
                Result theResult = reader.decode(bitmap, HINTS);
                if (theResult != null) {
                    results.add(theResult);
                }
            } catch (ReaderException re) {
                savedException = re;
            }
        }

        if (results.isEmpty()) {
            try {
                // Try again with other binarizer
                BinaryBitmap hybridBitmap = new BinaryBitmap(new HybridBinarizer(source));
                Result theResult = reader.decode(hybridBitmap, HINTS);
                if (theResult != null) {
                    results.add(theResult);
                }
            } catch (ReaderException re) {
                savedException = re;
            }
        }

        for (Result result : results) {
            contents.add(result.getText());
            FileOutputStream fos = new FileOutputStream(targetDirectory + "/foo.txt.xz");
            fos.write(decoder.decode(result.getText().getBytes()));
            fos.close();
        }

        return true;
    }
}
