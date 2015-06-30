package software.schipplock.qrcodebackup;

import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import org.apache.commons.cli.*;
import org.apache.pdfbox.exceptions.COSVisitorException;
import software.schipplock.qrcodebackup.utils.FileHelper;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws NotFoundException {

        HelpFormatter formatter = new HelpFormatter();

        Options options = new Options();
        options.addOption(OptionBuilder.withDescription("decode given qr code").create("d"));
        options.addOption(OptionBuilder.withArgName("c").withDescription("create qr code(s) for the provided filename").create("c"));

        options.addOption(OptionBuilder.withArgName("filename")
                        .hasArg(true)
                        .withDescription("filename (can be any file in case of qr code creation or a qr code file")
                        .isRequired(true)
                        .create("f")
        );

        options.addOption(OptionBuilder.withArgName("target directory")
                        .hasArg(true)
                        .withDescription("target directory where the files will be written to")
                        .isRequired(true)
                        .create("t")
        );

        CommandLineParser parser = new BasicParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            formatter.printHelp("qrcodebackup", options);
            System.exit(1);
        }

        if (!cmd.hasOption("c") && !cmd.hasOption("d")) {
            formatter.printHelp("qrcodebackup", options);
            System.exit(1);
        }

        String filename = cmd.getOptionValue("f");
        String targetDirectory = cmd.getOptionValue("t");

        if (!FileHelper.fileExists(filename)) {
            System.err.println("filename " + filename + " does not exist or isn't readable");
            System.exit(1);
        }

        QrCodeHelper qrCodeQrCodeHelper = new QrCodeHelper(filename);

        if (cmd.hasOption("c")) {
            try {
                qrCodeQrCodeHelper.generate(targetDirectory);
            } catch (IOException e) {
                System.err.println("could not generate qr code(s) for " + filename);
                e.printStackTrace();
            } catch (WriterException e) {
                System.err.println("could not generate qr code(s) for " + filename);
                e.printStackTrace();
            } catch (COSVisitorException e) {
                e.printStackTrace();
            }
        }

        if (cmd.hasOption("d")) {
            try {
                System.out.println(qrCodeQrCodeHelper.decode(targetDirectory));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
