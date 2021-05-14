package com.mitsui.fastgit.util;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.net.MalformedURLException;

public class XmlUtil {
    public static String toXml(Object obj) {
        XStream xstream = new XStream();
        xstream.processAnnotations(obj.getClass());
        return xstream.toXML(obj);
    }

    public static <T> T toBeanByXmlStr(String xmlStr, Class<T> cls) {
        XStream xstream = new XStream(new DomDriver());
        xstream.ignoreUnknownElements();
        xstream.processAnnotations(cls);
        T obj = (T) xstream.fromXML(xmlStr);
        return obj;
    }

    public static boolean toXMLFile(Object obj, String absPath, String fileName) {
        String strXml = toXml(obj);
        String filePath = absPath + fileName;
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        FileOutputStream ous = null;

        boolean flag;
        try {
            ous = new FileOutputStream(file);
            ous.write(strXml.getBytes());
            ous.flush();
            return true;
        } catch (Exception e) {
            flag = false;
        } finally {
            if (ous != null) {
                try {
                    ous.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return flag;
    }

    public static <T> T toBeanByXmlFileFromResouce(String filePath, Class<T> cls) {
        InputStream inStream = XmlUtil.class.getClassLoader().getResourceAsStream(filePath);
        InputStreamReader reader = new InputStreamReader(inStream);
        XStream xstream = new XStream(new DomDriver());
        xstream.ignoreUnknownElements();
        xstream.processAnnotations(cls);
        T obj = (T) xstream.fromXML(reader);
        return obj;
    }

    public static <T> T toBeanByXmlFile(String filePath, Class<T> cls) throws FileNotFoundException {
        InputStream inStream = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(inStream);
        XStream xstream = new XStream(new DomDriver());
        xstream.ignoreUnknownElements();
        xstream.processAnnotations(cls);
        T obj = (T) xstream.fromXML(reader);
        return obj;
    }

    public static Document read(String filePath) throws MalformedURLException, DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(filePath));
        return document;
    }

    public static Element getRootElement(Document doc) {
        return doc.getRootElement();
    }

    public Element getRootElement(String filePath) throws MalformedURLException, DocumentException {
        Document doc = read(filePath);
        return getRootElement(doc);
    }

    public static Element findNode(Element element, String nodeName) {
        return element.element(nodeName);
    }

    public static void write(Document document, String filePath) throws IOException {
        XMLWriter writer = new XMLWriter(new FileWriter(filePath));
        writer.write(document);
        writer.close();
    }

}
