/**
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.tuscany.sdo.test;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.tuscany.sdo.helper.XMLStreamHelper;
import org.apache.tuscany.sdo.util.SDOUtil;

import commonj.sdo.DataObject;
import commonj.sdo.helper.*;

public class XMLStreamHelperTestCase extends TestCase {

    private HelperContext hc;

    private XMLStreamHelper streamHelper;

    private XMLInputFactory inputFactory;

    private XMLOutputFactory outputFactory;

    private QName module = new QName("http://foo", "module");

    private QName name = new QName("http://bar", "implementation.mock");

    private String testName = "foo-ext";
    // private String testName = "complex";

    private String xml;

    // = "<module name=\"m\" xmlns=\"http://foo\" xmlns:bar=\"http://bar\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"
    // xsi:schemaLocation=\"http://bar foo-ext.xsd http://foo foo.xsd \"><component name=\"c\"><bar:implementation.mock
    // myAttr=\"helloworld.HelloWorldImpl\" listAttr=\"1 2\">1<bar:myElement>Dummy</bar:myElement>2</bar:implementation.mock></component></module>";

    protected void setUp() throws Exception {
        super.setUp();
        
        hc = SDOUtil.createHelperContext();
        streamHelper = SDOUtil.createXMLStreamHelper(hc.getTypeHelper());

        URL url = getClass().getClassLoader().getResource(testName + ".xsd");
        hc.getXSDHelper().define(url.openStream(), url.toExternalForm());
        url = getClass().getResource("/mixed.xsd");
        hc.getXSDHelper().define(url.openStream(), url.toString());

        inputFactory = XMLInputFactory.newInstance();
        outputFactory = XMLOutputFactory.newInstance();
        
        url = getClass().getClassLoader().getResource(testName + ".xml");
        InputStreamReader reader = new InputStreamReader(url.openStream());
        StringBuffer stringBuffer = new StringBuffer();
        char buf[] = new char[1024];
        int size;
        while ((size = reader.read(buf)) != -1)
            stringBuffer.append(buf, 0, size);
        xml = stringBuffer.toString();
        reader.close();
    }

    public void testLoadObject() throws Exception {
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xml));
        int event = reader.getEventType();
        while (!(event == XMLStreamConstants.START_ELEMENT && reader.getName().equals(name)) && reader.hasNext()) {
            event = reader.next();
        }
        DataObject dataObject = streamHelper.loadObject(reader);
        Assert.assertNotNull(dataObject);
        Assert.assertTrue(dataObject.getString("myAttr").equals("helloworld.HelloWorldImpl"));
    }

    public void testLoadUnqualifiedObject() throws Exception {
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xml.replaceAll("bar:", "")));
        int event = reader.getEventType();
        while (!(event == XMLStreamConstants.START_ELEMENT && reader.getName().getLocalPart().equals(name.getLocalPart())) && reader.hasNext()) {
            event = reader.next();
        }
        Map options = new HashMap();
        options.put(XMLStreamHelper.OPTION_DEFAULT_ROOT_TYPE, hc.getTypeHelper().getType(name.getNamespaceURI(), "MockImplementation"));
        DataObject dataObject = streamHelper.loadObject(reader, options);
        Assert.assertNotNull(dataObject);
        Assert.assertTrue(dataObject.getString("myAttr").equals("helloworld.HelloWorldImpl"));
    }

    public void testLoad() throws Exception {
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xml));
        XMLDocument document = streamHelper.load(reader);
        Assert.assertNotNull(document);
        Assert.assertEquals(document.getRootElementURI(), module.getNamespaceURI());
        Assert.assertEquals(document.getRootElementName(), module.getLocalPart());
        DataObject moduleObject = document.getRootObject();
        List components = moduleObject.getList("component");
        DataObject componentObject = (DataObject) components.get(0);
        DataObject implObject = componentObject.getDataObject("implementation.mock");
        Assert.assertTrue(implObject.getString("myAttr").equals("helloworld.HelloWorldImpl"));
    }

    public void testSave() throws XMLStreamException {
        XMLDocument document = hc.getXMLHelper().load(xml);
        StringWriter writer = new StringWriter();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        streamHelper.save(document, streamWriter);
        streamWriter.flush();
        String xmlStr = writer.toString();
        //System.out.println(xmlStr);
        Assert.assertTrue(xmlStr.indexOf("myAttr=\"helloworld.HelloWorldImpl\"")!=-1);
    }

    public void testSaveObject() throws XMLStreamException {
        XMLDocument document = hc.getXMLHelper().load(xml);
        DataObject moduleObject = document.getRootObject();
        List components = moduleObject.getList("component");
        DataObject componentObject = (DataObject) components.get(0);
        StringWriter writer = new StringWriter();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        streamHelper.saveObject(componentObject, streamWriter);
        streamWriter.flush();
        Assert.assertTrue(writer.toString().indexOf("myAttr=\"helloworld.HelloWorldImpl\"")!=-1);
    }

    public void testSaveSequence() throws IOException, XMLStreamException {
        DataObject quote = hc.getDataFactory().create("http://www.example.com/mixed", "MixedQuote");
        quote.setString("symbol", "fbnt");
        quote.getSequence().addText(0, "testing");
        quote.getSequence().addText("more testing");

        StringWriter writer = new StringWriter();
        XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
        streamHelper.saveObject(quote, streamWriter);
        streamWriter.flush();
        //System.out.println(writer);
        assertTrue(writer.toString().indexOf("<symbol>fbnt</symbol>") != -1);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}