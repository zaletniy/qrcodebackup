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
import javax.imageio.stream.ImageInputStream;
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

    private String author;
    private String filename;
    private String title;
    private String version;

    private static final Map<DecodeHintType,Object> HINTS;
    private static final Map<DecodeHintType,Object> HINTS_PURE;

    static {
        HINTS = new EnumMap<>(DecodeHintType.class);
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
        HINTS_PURE = new EnumMap<>(HINTS);
        HINTS_PURE.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }

    public QrCodeHelper(String author, String filename, String title, String version) {
        this.author = author;
        this.filename = filename;
        this.title = title;
        this.version = version;
    }

    private int getMaxByte() {
        return this.maxByte;
    }

    private String getFilename() {
        return this.filename;
    }

    private String getAuthor() {
        return this.author;
    }

    private String getTitle() {
        return this.title;
    }

    private String getVersion() {
        return this.version;
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

        // check the maximum file size here
        File base64EncodedFile = new File(targetFile + ".xz.base64");
        System.out.println(base64EncodedFile.length());
        if (base64EncodedFile.length() > this.getMaxByte()) {
            System.err.println("file is " + (base64EncodedFile.length() - this.getMaxByte()) + " bytes too big; limit is: " + this.getMaxByte() + " bytes");
            System.exit(2);
        }

        // create the qr code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        BufferedInputStream f = new BufferedInputStream(new FileInputStream(base64EncodedFile));
        byte[] buffer = new byte[this.getMaxByte()];
        f.read(buffer, 0, this.getMaxByte());
        String content = new String(buffer);
        long sourceFileSize = base64EncodedFile.length();

        BitMatrix byteMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 800, 800, hintMap);
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
        returnValue = ImageIO.write(image, "png", base64EncodedFile);

        if (returnValue) {
            // create pdf so we can easily print it
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            // 100mm x 150mm at 72dpi
            page.setMediaBox(new PDRectangle(100*(1/(10*2.54f)*72), 150*(1/(10*2.54f)*72)));
            document.addPage(page);

            PDFont font = PDType1Font.COURIER;
            PDFont boldFont = PDType1Font.COURIER_BOLD;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // add _the_ image
            PDXObjectImage ximage = new PDPixelMap(document, image);
            float scale = 0.381f;
            contentStream.drawXObject(ximage, -11, 131, ximage.getWidth() * scale, ximage.getHeight() * scale);

            contentStream.drawLine(5, 141, 276, 141);

            // add the logo
            float logoScale = 0.28f;
            ImageInputStream logoImg = ImageIO.createImageInputStream(this.getClass().getResourceAsStream("assets/logo.png"));
            PDXObjectImage logoImage = new PDPixelMap(document, ImageIO.read(logoImg));

            contentStream.drawXObject(logoImage, 25, 82, logoImage.getWidth() * logoScale, logoImage.getHeight() * logoScale);

            contentStream.drawLine(5, 75, 276, 75);

            // add some info text
            contentStream.beginText();
            contentStream.setFont(boldFont, 12);
            contentStream.moveTextPositionByAmount(5, 60);
            contentStream.drawString(this.getTitle());
            contentStream.setFont(font, 9);
            contentStream.moveTextPositionByAmount(0, -15);
            contentStream.drawString("Aktoro    : " + this.getAuthor());
            contentStream.moveTextPositionByAmount(0, -9);
            contentStream.drawString("Versio    : " + this.getVersion());
            contentStream.moveTextPositionByAmount(0, -9);
            contentStream.drawString("Formato   : " + basename + ">base64>xz(1.0.4)");
            contentStream.moveTextPositionByAmount(0, -9);
            contentStream.drawString("Dato      : " + LocalDate.now());
            contentStream.moveTextPositionByAmount(0, -9);
            contentStream.drawString("Grandeco  : " + sourceFileSize + " Bytes");
            contentStream.endText();

            contentStream.close();

            document.save(targetFile.replace(".java", "_" + this.getVersion() + ".pdf"));
            document.close();
        }

        // cleanup
        FileHelper.delete(targetFile + ".xz");
        FileHelper.delete(targetFile + ".xz.base64");

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
