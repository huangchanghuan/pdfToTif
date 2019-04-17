import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.TIFFEncodeParam;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TifMerge {
    private static String PDF_PATH;
    private static String TIF_PATH;
    private static String RESULT_PATH;

    public static void main(String[] args) throws Exception {
        //获取系统桌面路径
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        String desktopPath = desktopDir.getAbsolutePath();
        //创建路径下面的文件夹 pdf tif result
        File file = new File(desktopPath + "\\tif");
        if (!file.exists()) {//如果文件夹不存在
            file.mkdir();//创建文件夹
        }
        File file1 = new File(desktopPath + "\\result");
        if (!file1.exists()) {//如果文件夹不存在
            file1.mkdir();//创建文件夹
        }
        //初始化路径参数
        PDF_PATH = desktopPath + "\\pdf";
        TIF_PATH = desktopPath + "\\tif";
        RESULT_PATH = desktopPath + "\\result";
        // 遍历文件夹下的pdf
        traverseFolder2(PDF_PATH);
    }

    /**
     * 遍历文件夹
     *
     * @param path
     */
    public static void traverseFolder2(String path) throws Exception {
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (null == files || files.length == 0) {
                System.out.println("文件夹是空的!");
                return;
            } else {
                for (File file2 : files) {
                    if (file2.isDirectory()) {
                        System.out.println("文件夹:" + file2.getAbsolutePath());
                        traverseFolder2(file2.getAbsolutePath());
                    } else {
                        System.out.println("文件:" + file2.getAbsolutePath());
                        //pdf转成tif
                        List<String> bookFilePaths = img(file2.getAbsolutePath(), TIF_PATH);
                        //tif进行合并
                        many2one(bookFilePaths, RESULT_PATH, file2.getName().replace("pdf", "tif"));
                        System.out.println("转换成功:" + file2.getName());
                    }
                }
            }
        } else {
            System.out.println("文件不存在!");
        }
    }

    /**
     * 合并
     *
     * @param bookFilePaths
     * @param toPath
     * @param distFileName  合并文件名 包含后缀
     */
    public static void many2one(List<String> bookFilePaths, String toPath, String distFileName) {
        if (bookFilePaths != null && bookFilePaths.size() > 0) {
            File[] files = new File[bookFilePaths.size()];
            for (int i = 0; i < bookFilePaths.size(); i++) {
                files[i] = new File(bookFilePaths.get(i));
            }
            if (files != null && files.length > 0) {

                try {
                    ArrayList pages = new ArrayList(files.length - 1);
                    FileSeekableStream[] stream = new FileSeekableStream[files.length];
                    for (int i = 0; i < files.length; i++) {
                        stream[i] = new FileSeekableStream(
                                files[i].getCanonicalPath());
                    }
                    ParameterBlock pb = (new ParameterBlock());
                    PlanarImage firstPage = JAI.create("stream", stream[0]);
                    for (int i = 1; i < files.length; i++) {
                        PlanarImage page = JAI.create("stream", stream[i]);
                        pages.add(page);

                    }
                    TIFFEncodeParam param = new TIFFEncodeParam();
                    // 设置压缩方式
                    param.setCompression(TIFFEncodeParam.COMPRESSION_JPEG_TTN2);
                    File f = new File(toPath);
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                    OutputStream os = new FileOutputStream(toPath + File.separator + distFileName);
                    ImageEncoder enc = ImageCodec.createImageEncoder("tiff",
                            os, param);
                    param.setExtraImages(pages.iterator());
                    enc.encode(firstPage);
                    for (int i = 0; i < files.length; i++) {
                        stream[i].close();
                        if (files[i].isFile() && files[i].exists()) {
                            files[i].delete();
                        }
                    }
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 转换成tif
     *
     * @throws Exception
     */
    public static List<String> img(String pdfPath, String tifPath) throws Exception {
        List<String> tifPathList = new ArrayList<>();
// 定义Document,用于转换图片
        Document document = new Document();
// 找到路径
        document.setFile(pdfPath);
// 用来保存当前页码的页码
        Map<String, Integer> map = new HashMap<String, Integer>();
        float rotation = 0f;
// 获取这个pdf的页码一共多少页
        int maxPages = document.getNumberOfPages();
// 循环pdf每一页并转换
        for (int i = 0; i < document.getNumberOfPages(); i++) {

            BufferedImage image = (BufferedImage) document.getPageImage(i,
                    GraphicsRenderingHints.SCREEN, Page.BOUNDARY_CROPBOX,
                    rotation, (float) 3.0);
            BufferedImage bufferedImage = new BufferedImage(
                    image.getWidth(null), image.getHeight(null),
// BufferedImage.TYPE_INT_RGB ： 表示一个图像，该图像具有整数像素的 8 位 RGB 颜色
                    BufferedImage.TYPE_INT_RGB);
            bufferedImage.getGraphics().drawImage(image, 0, 0, null);
//图片的输出全路径
            String everyTifPath = tifPath + "\\" + i + ".tif";
            OutputStream os = new FileOutputStream(everyTifPath);
            tifPathList.add(everyTifPath);
            TIFFEncodeParam param = new TIFFEncodeParam();
// 设置压缩方式
            param.setCompression(TIFFEncodeParam.COMPRESSION_JPEG_TTN2);
//转换成指定的格式。
            ImageEncoder enc = ImageCodec.createImageEncoder("TIFF", os,
                    param);
            enc.encode(bufferedImage);
            os.close();
            image.flush();
        }
        document.dispose();
        map.put("pageCount", maxPages);
        return tifPathList;
    }
}
