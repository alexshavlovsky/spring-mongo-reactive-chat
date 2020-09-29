package com.ctzn.springmongoreactivechat.service.thumbs;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

@Component
public class PdfThumbBuilder implements ThumbBuilderResolver {

    @Override
    public Thumbnails.Builder resolve(String thumbType, DataBuffer dataBuffer) throws Exception {
        if (!"pdf".equals(thumbType)) return null;
        PDFFile pdf = new PDFFile(dataBuffer.asByteBuffer());
        if (pdf.getNumPages() == 0) throw new Exception("PDF renderer: Can't parse any page");
        PDFPage page = pdf.getPage(0);
        Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
        BufferedImage bufferedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB);
        Image image = page.getImage(rect.width, rect.height, rect, null, true, true);
        Graphics2D bufImageGraphics = bufferedImage.createGraphics();
        bufImageGraphics.drawImage(image, 0, 0, null);
        return Thumbnails.of(bufferedImage);
    }
}
