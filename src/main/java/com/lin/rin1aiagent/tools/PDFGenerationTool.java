package com.lin.rin1aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.lin.rin1aiagent.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 生成工具
 * 支持将文本内容写入 PDF 文件，自动处理中文和换行
 */
@Slf4j
public class PDFGenerationTool {

    private static final String PDF_DIR = FileConstant.FILE_SAVE_DIR + "/pdf";
    /** 页面左右边距 */
    private static final float MARGIN = 50;
    /** 正文字号 */
    private static final float FONT_SIZE = 12;
    /** 标题字号 */
    private static final float TITLE_FONT_SIZE = 18;
    /** 行间距倍数 */
    private static final float LINE_SPACING = 1.5f;

    /**
     * 生成 PDF 文件
     *
     * @param fileName 文件名（不含扩展名）
     * @param title    PDF 标题，显示在首页顶部
     * @param content  正文内容，支持 \n 换行
     * @return 生成结果描述（成功返回文件路径，失败返回错误信息）
     */
    @Tool(description = "Generate a PDF file with the given title and content. Supports Chinese text and automatic line wrapping.")
    public String generatePDF(
            @ToolParam(description = "File name without extension, e.g. 'report'") String fileName,
            @ToolParam(description = "Title displayed at the top of the PDF") String title,
            @ToolParam(description = "Main content of the PDF, use \\n for line breaks") String content
    ) {
        FileUtil.mkdir(PDF_DIR);
        String filePath = PDF_DIR + "/" + fileName + ".pdf";

        try (PDDocument document = new PDDocument()) {
            // 加载支持中文的字体（使用系统字体）
            PDType0Font font = loadChineseFont(document);
            PDType0Font boldFont = font; // PDFBox 开源版无法直接加粗，复用同一字体

            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float contentWidth = pageWidth - 2 * MARGIN;

            // 将内容按行分割，并处理自动换行
            List<String> lines = wrapLines(content, font, FONT_SIZE, contentWidth);

            // 分页写入
            float y = pageHeight - MARGIN;
            PDPage page = null;
            PDPageContentStream stream = null;

            // 写标题
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = pageHeight - MARGIN;

            // 标题居中
            float titleWidth = boldFont.getStringWidth(title) / 1000 * TITLE_FONT_SIZE;
            float titleX = (pageWidth - titleWidth) / 2;
            stream.beginText();
            stream.setFont(boldFont, TITLE_FONT_SIZE);
            stream.newLineAtOffset(titleX, y);
            stream.showText(title);
            stream.endText();
            y -= TITLE_FONT_SIZE * LINE_SPACING * 2;

            // 写正文
            stream.beginText();
            stream.setFont(font, FONT_SIZE);
            stream.newLineAtOffset(MARGIN, y);
            float lineHeight = FONT_SIZE * LINE_SPACING;

            for (String line : lines) {
                // 当前页剩余空间不足，换页
                if (y - lineHeight < MARGIN) {
                    stream.endText();
                    stream.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    y = pageHeight - MARGIN;

                    stream.beginText();
                    stream.setFont(font, FONT_SIZE);
                    stream.newLineAtOffset(MARGIN, y);
                }
                stream.showText(line);
                stream.newLineAtOffset(0, -lineHeight);
                y -= lineHeight;
            }

            stream.endText();
            stream.close();

            document.save(filePath);
            log.info("PDF 生成成功: {}", filePath);
            return "PDF generated successfully: " + filePath;

        } catch (Exception e) {
            log.error("PDF 生成失败: {}", fileName, e);
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 加载中文字体，优先使用系统字体，找不到则回退到内置字体
     */
    private PDType0Font loadChineseFont(PDDocument document) throws Exception {
        // Windows 系统字体路径
        String[] fontPaths = {
                "C:/Windows/Fonts/simhei.ttf",   // 黑体
                "C:/Windows/Fonts/simsun.ttc",   // 宋体
                "C:/Windows/Fonts/msyh.ttc",     // 微软雅黑
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",  // Linux
                "/System/Library/Fonts/PingFang.ttc"              // macOS
        };
        for (String path : fontPaths) {
            File fontFile = new File(path);
            if (fontFile.exists()) {
                log.debug("使用字体: {}", path);
                return PDType0Font.load(document, fontFile);
            }
        }
        // 回退：使用 classpath 内置字体（需自行放置）
        InputStream fontStream = getClass().getResourceAsStream("/fonts/simhei.ttf");
        if (fontStream != null) {
            return PDType0Font.load(document, fontStream);
        }
        throw new IllegalStateException("未找到可用的中文字体，请确保系统已安装中文字体");
    }

    /**
     * 将文本按最大宽度自动换行，同时保留原始 \n 换行
     */
    private List<String> wrapLines(String content, PDType0Font font, float fontSize, float maxWidth) throws Exception {
        List<String> result = new ArrayList<>();
        for (String paragraph : content.split("\n", -1)) {
            if (paragraph.isBlank()) {
                result.add("");
                continue;
            }
            // 逐字符检测宽度，超出则换行
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                current.append(paragraph.charAt(i));
                float width = font.getStringWidth(current.toString()) / 1000 * fontSize;
                if (width > maxWidth) {
                    // 退一个字符，当前行结束
                    result.add(current.substring(0, current.length() - 1));
                    current = new StringBuilder();
                    current.append(paragraph.charAt(i));
                }
            }
            if (!current.isEmpty()) {
                result.add(current.toString());
            }
        }
        return result;
    }
}
