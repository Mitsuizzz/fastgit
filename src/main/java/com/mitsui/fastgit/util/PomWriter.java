package com.mitsui.fastgit.util;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

public class PomWriter {
    private String pomPath;
    private Document document;
    private Element root;

    public PomWriter(String pomPath) throws MalformedURLException, DocumentException {
        this.pomPath = pomPath;
        this.document = XmlUtil.read(pomPath);
        this.root = XmlUtil.getRootElement(this.document);
    }

    public void setParentVersion(String version) {
        Element parentNode = XmlUtil.findNode(this.root, "parent");
        if (parentNode != null) {
            Element parentVersionNode = XmlUtil.findNode(parentNode, "version");
            if (parentNode != null) {
                parentVersionNode.setText(version);
            }
        }
    }

    public void setVersion(String version) {
        Element versionNode = XmlUtil.findNode(this.root, "version");
        if (versionNode != null) {
            versionNode.setText(version);
        }
    }

    public void setProperties(Map<String, String> properties) {
        Element propertiesNode = XmlUtil.findNode(this.root, "properties");
        if (propertiesNode != null) {
            for (Map.Entry<String, String > entry : properties.entrySet()) {
                Element node = XmlUtil.findNode(propertiesNode, entry.getKey());
                if (node != null) {
                    node.setText(entry.getValue());
                }
            }
        }
    }

    public void write() throws IOException {
        XmlUtil.write(this.document, this.pomPath);
    }
}
