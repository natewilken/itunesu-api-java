/*
 * Copyright (c) 2007, Arizona State University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Arizona State University nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY ARIZONA STATE UNIVERSITY ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ARIZONA STATE UNIVERSITY BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.asu.itunesu;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An entire iTunesU site, composed of many {@link Section} objects.
 * <br><br>
 * Note: Although this class contains writable properties, the iTunesU API
 * does not currently provide any method to update site-level information.
 */
public class Site implements ITunesUElement {
    private String name;
    private String handle;
    private Boolean allowSubscription;
    private List<Permission> permissions;
    private List<Section> sections;
    private Templates templates;

    public Site() {
        this.permissions = new ArrayList<Permission>();
        this.sections = new ArrayList<Section>();
    }

    public Site(String name,
                String handle,
                Boolean allowSubscription,
                List<Permission> permissions,
                List<Section> sections,
                Templates templates) {
        this.name = name;
        this.handle = handle;
        this.allowSubscription = allowSubscription;
        this.permissions = permissions;
        this.sections = sections;
        this.templates = templates;
    }

    public String getName() {
        return this.name;
    }

    public String getHandle() {
        return this.handle;
    }

    public Boolean getAllowSubscription() {
        return this.allowSubscription;
    }

    public List<Permission> getPermissions() {
        return this.permissions;
    }

    public List<Section> getSections() {
        return this.sections;
    }

    public Templates getTemplates() {
        return this.templates;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public void setAllowSubscription(Boolean allowSubscription) {
        this.allowSubscription = allowSubscription;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public void setTemplates(Templates templates) {
        this.templates = templates;
    }

    public Element toXmlElement(Document doc) {
        Element element = doc.createElement("Site");
        if (this.name != null) {
            Element nameElement = doc.createElement("Name");
            nameElement.setTextContent(this.name);
            element.appendChild(nameElement);
        }
        if (this.handle != null) {
            Element handleElement = doc.createElement("Handle");
            handleElement.setTextContent(this.handle);
            element.appendChild(handleElement);
        }
        if (this.allowSubscription != null) {
        	Element allowSubscriptionElement =
        		doc.createElement("AllowSubscription");
        	allowSubscriptionElement.setTextContent(this.allowSubscription
                                                	? "true" : "false");
        	element.appendChild(allowSubscriptionElement);
        }
        for (Permission permission : this.permissions) {
            element.appendChild(permission.toXmlElement(doc));
        }
        for (Section section : this.sections) {
            element.appendChild(section.toXmlElement(doc));
        }
        if (this.templates != null) {
            element.appendChild(this.templates.toXmlElement(doc));
        }
        return element;
    }

    public String toXml()
        throws ParserConfigurationException,
               TransformerException {
        DocumentBuilderFactory docFactory =
            DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        
        doc.appendChild(this.toXmlElement(doc));
        
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer trans = transFactory.newTransformer();
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        
        return writer.toString();
    }
    
    public static Site fromXmlElement(Element element) throws ITunesUException {
        if (!"Site".equals(element.getNodeName())) {
            throw new ITunesUException("Expected Site, got "
                                       + element.getNodeName());
        }
        String name = null;
        String handle = null;
        Boolean allowSubscription = false;
        List<Permission> permissions = new ArrayList<Permission>();
        List<Section> sections = new ArrayList<Section>();
        Templates templates = null;
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if ("Name".equals(childNode.getNodeName())) {
                    name = childNode.getTextContent();
                } else if ("Handle".equals(childNode.getNodeName())) {
                    handle = childNode.getTextContent();
                } else if ("AllowSubscription".equals(childNode.getNodeName())) {
                    allowSubscription = "true".equals(childNode.getTextContent());
                } else if ("Permission".equals(childNode.getNodeName())) {
                    permissions.add(Permission.fromXmlElement((Element) childNode));
                } else if ("Section".equals(childNode.getNodeName())) {
                    sections.add(Section.fromXmlElement((Element) childNode));
                } else if ("Templates".equals(childNode.getNodeName())) {
                    templates = Templates.fromXmlElement((Element) childNode);
                }
            }
        }
        return new Site(name,
                        handle,
                        allowSubscription,
                        permissions,
                        sections,
                        templates);
    }

    public static Site fromXml(String xml)
        throws ITunesUException {
        DocumentBuilderFactory docFactory =
            DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ITunesUException(e);
        }
        Document doc;
        try {
            doc = docBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException e) {
            throw new ITunesUException(e);
        } catch (IOException e) {
            throw new ITunesUException(e);
        }
        return Site.fromXmlElement(doc.getDocumentElement());
    }

    public String toString() {
    	return (super.toString()
                + "[name="
                + (this.getName() == null ? "<null>" : this.getName())
                + ",handle="
                + (this.getHandle() == null ? "<handle>" : this.getHandle())
                + "]");
    }
}
