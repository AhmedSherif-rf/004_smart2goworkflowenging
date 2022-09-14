/*

 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER. Copyright 1997-2020 NTG Clarity and/or its affiliates. All

 * rights reserved. NTG CLARITY is a leader in delivering network, telecom, IT and infrastructure solutions to network

 * service providers and medium and large enterprises. www.ntgclarity.com The contents of this file are subject to the

 * terms of "NTG Clarity License". You must not use this file except in compliance with the License. You can obtain a

 * copy of the License at http://www.ntgclarity.com/ See the License for the specific language governing permissions and

 * limitations under the License. Contributor(s): The Initial Developer of the Original Software is NTG Clarity . , Inc.

 * Copyright 1997-2020 NTG Clarity. All Rights Reserved. CLASS NAME <h4>Description</h4> <h4>Notes</h4>

 * <h4>References</h4>

 * @author: ${CITIES} <A HREF="mailto:[ntg.support@ntgclarity.com]">NTG Clarity Support Team</A>

 * @version Revision: 1.0.1 Date: ${date} ${time}

 * @see [String]

 * @see [URL]

 * @see [Class name#method name]

 */
package com.ntg.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.http.HttpSessionListener;

import com.ntg.common.NTGEncryptor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.web.client.RestTemplate;

import com.ntg.common.NTGMessageOperation;
import com.ntg.common.STAGESystemOut;
import com.ntg.engine.exceptions.NTGException;


@SpringBootApplication
@ComponentScan(basePackages = {"com.ntg.engine"})
public class Application implements EnvironmentAware {

    public static void main(String[] args) {

        try {
            System.out.println("******************************");
            System.out.println("NTG Init ..............");
            System.out.println("******************************");
            STAGESystemOut.OverrideSystemOutput();

            SpringApplication.run(Application.class, args);
            NTGInit();
        } catch (Throwable e) {
            NTGMessageOperation.PrintErrorTrace(new Exception(e));
            throw e;
        }
    }

    private Environment env;

    @Override
    public void setEnvironment(final Environment env) {
        this.env = env;
    }

    static String Version;

    public static void NTGInit() {
        STAGESystemOut.OverrideSystemErrorOutput();

        System.out.println("*********************************************************");
        System.out.println("   _____                      __ ___   ______    \n" +
                "  / ___/____ ___  ____ ______/ /|__ \\ / ____/___ \n" +
                "  \\__ \\/ __ `__ \\/ __ `/ ___/ __/_/ // / __/ __ \\\n" +
                " ___/ / / / / / / /_/ / /  / /_/ __// /_/ / /_/ /\n" +
                "/____/_/ /_/ /_/\\__,_/_/   \\__/____/\\____/\\____/ \n" +
                "                                                 ");
        System.out.println(" _       __           __   ______                 ______            _          \n" +
                "| |     / /___  _____/ /__/ __/ /___ _      __   / ____/___  ____ _(_)___  ___ \n" +
                "| | /| / / __ \\/ ___/ //_/ /_/ / __ \\ | /| / /  / __/ / __ \\/ __ `/ / __ \\/ _ \\\n" +
                "| |/ |/ / /_/ / /  / ,< / __/ / /_/ / |/ |/ /  / /___/ / / / /_/ / / / / /  __/\n" +
                "|__/|__/\\____/_/  /_/|_/_/ /_/\\____/|__/|__/  /_____/_/ /_/\\__, /_/_/ /_/\\___/ \n" +
                "                                                          /____/               ");
        System.out.println(":: WFEngine  Started:: (v" + Version + ")");
        System.out.println("*********************************************************");
    }

    @Bean
    public PropertyPlaceholderConfigurer properties() throws Exception {
        final PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        System.err.println("creating External Config");
        CreateExternalPropertyFile();
        intializeSettings("quartzWF.properties", false, false);
        Properties prop = intializeSettings("applicationWFEngin.properties", false, false);
        ppc.setIgnoreResourceNotFound(true);
        final List<Resource> resourceLst = new ArrayList<Resource>();
        String homePath = System.getProperty("user.home");
        resourceLst.add(new FileSystemResource(homePath + "/.Smart2GoConfig/.quartzWF.properties"));
        ppc.setProperties(prop);
        ppc.setLocations(resourceLst.toArray(new Resource[]{}));

        return ppc;
    }

    public static void CreateExternalPropertyFile() {
        try {
            String path = System.getProperty("user.home") + "/.Smart2GoConfig";
            File file = new File(path);
            if (!file.exists()) {
                Path pathToCreate = Paths.get(path);
                Files.createDirectories(pathToCreate);
            }
            path = System.getProperty("user.home") + "/.Smart2GoConfig/.quartzWF.properties";
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            path = System.getProperty("user.home") + "/.Smart2GoConfig/.applicationWFEngin.properties";
            file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }

        } catch (IOException e) {
            NTGMessageOperation.PrintErrorTrace(e);
        }
    }

    @Bean(name = "restTemplate")
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }


    public static void intializeSettingsconfig(String PropertyFileName, boolean JustCopy) throws Exception {
        Properties props = new Properties();
        InputStream input = Application.class.getClassLoader().getResourceAsStream(PropertyFileName);
        props.load(input);
        if (props != null) {
            Properties propsToWrite = new Properties();
            Properties oldProps = new Properties();

            String path = System.getProperty("user.home") + "/.Smart2GoConfig/." + PropertyFileName;
            File f = new File(path);
            oldProps.load(new FileInputStream(f));
            for (Object property : props.keySet()) {
                String properyName = property.toString().replaceAll("\\[", "").replaceAll("\\]", "");

                if (System.getenv(properyName) != null) {
                    propsToWrite.put(property, System.getenv(properyName));
                } else if (System.getProperty(property.toString()) != null) {
                    propsToWrite.put(property, System.getProperty(property.toString()));
                } else {
                    if (oldProps.get(property) != null) {
                        propsToWrite.put(property, oldProps.get(property));
                    } else {
                        propsToWrite.put(property, props.get(property));
                    }
                }
            }

            // get values in the custom property not in the orginal config

            for (Object property : oldProps.keySet()) {
                String properyName = property.toString().replaceAll("\\[", "").replaceAll("\\]", "");

                if (System.getenv(properyName) != null) {
                    propsToWrite.put(property, System.getenv(properyName));
                } else {
                    propsToWrite.put(property, oldProps.get(property));
                }
            }

            OutputStream out = new FileOutputStream(f);
            // write into it
            DefaultPropertiesPersister p = new DefaultPropertiesPersister();
            p.store(propsToWrite, out, "NG STAGE " + PropertyFileName + " Propery File ");
            if (!JustCopy) {
                Object[] keys = propsToWrite.keySet().toArray();
                for (Object k : keys) {
                    String v = (String) propsToWrite.get(k);
                    System.setProperty((String) k, v);
                }
            }
        }
    }

    public static String getEnvVar(String prop) {
        Context initCtx;
        String value1 = null;
        try {
            initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:global/");
            value1 = (String) envCtx.lookup(prop);
        } catch (NamingException e) {
            NTGMessageOperation.PrintErrorTrace(e);
        }

        return value1;
    }

    // we may need this https://github.com/javamelody/javamelody/blob/without-spring-boot-starter/javamelody-for-spring-boot/src/main/java/hello/JavaMelodyConfiguration.java
    //yghandor
    /*https://github.com/javamelody/javamelody/wiki/SpringBootStarter
     * Spring beans having an annotation @Controller,
     * @RestController, @Service, and RestTemplate defined as bean, and since v1.68,
     *  beans and methods having an annotation @Async are automatically monitored. Otherwise,
     *  if you want to monitor method calls on some Spring beans, you can
     *  add @MonitoredWithSpring on those classes or methods.
     *
     * */
    @Bean
    public HttpSessionListener javaMelodyListener() {
        return new net.bull.javamelody.SessionListener();
    }

    @Bean
    public Filter javaMelodyFilter() {
        return new net.bull.javamelody.MonitoringFilter();
    }


    public static Properties intializeSettings(String PropertyFileName, boolean EscapeCopyOut, boolean IgnoreHomeResourceFile) throws Exception {


        Properties props = new Properties();
        Properties propsToWrite = null;
        InputStream input = Application.class.getClassLoader().getResourceAsStream(PropertyFileName);
        if (input == null) {
            System.out.println("Resource File " + PropertyFileName + " can't be Loaded");
            throw new NTGException("Sys001", "Resource File " + PropertyFileName + " can't be Loaded");
        }

        if (input != null) {
            props.load(input);
            propsToWrite = new Properties();
            Properties oldProps = new Properties();
            File f = null;
            if (IgnoreHomeResourceFile == false) {
                String path = System.getProperty("user.home") + "/.Smart2GoConfig/." + PropertyFileName;
                f = new File(path);
                if (f.exists()) {
                    oldProps.load(new FileInputStream(f));
                }
            }

            for (Object property : props.keySet()) {

                String properyName = property.toString().replaceAll("\\[", "").replaceAll("\\]", "");
                Object properyValue;
                if (System.getenv(properyName) != null) {
                    properyValue = System.getenv(properyName);

                } else {
                    if (oldProps.get(property) != null
                            && property.toString().startsWith("pom.") == false
                            && property.toString().startsWith("ImpExp.version") == false
                    ) {
                        properyValue = oldProps.get(property);
                    } else {
                        properyValue = props.get(property);
                        if (property.toString().equals("pom.version")) {
                            Version = (String) props.get(property);
                        }
                    }
                }
                ////Dev-00003395 : extract the encrypted value for the password
                String properyValuestr = properyValue.toString();
                if (property.toString().toLowerCase().contains("password")) {
                    int start = properyValuestr.toLowerCase().indexOf("enc(");
                    int end = properyValuestr.lastIndexOf(")");
                    if (start > -1 && end > 0) {

                        if (properyValuestr.trim().startsWith("${") && properyValuestr.trim().endsWith("}") && properyValuestr.contains(":")) {
                            int index = properyValuestr.indexOf(":");
                            String FirstPart = properyValuestr.substring(0, index + 1);

                            properyValuestr = properyValuestr.substring(start + 4, end);

                            properyValue = FirstPart + NTGEncryptor.decrypt(properyValuestr) + "}";
                        } else {

                            properyValuestr = properyValuestr.substring(start + 4, end);
                            properyValue = NTGEncryptor.decrypt(properyValuestr);
                        }
                    }
                }
                propsToWrite.put(property, properyValue);

                if (properyName.equals("app.instance-id")) {
                    if (propsToWrite.get(properyName).equals("${random.uuid}")) {
                        propsToWrite.put(properyName, "BE-" + java.util.UUID.randomUUID());
                    }
                }

            }

            // write into it
            if (EscapeCopyOut == false) {
                StoreTheNewPropertiyFile(propsToWrite, PropertyFileName, f);
            }

            Object[] keys = propsToWrite.keySet().toArray();
            for (Object k : keys) {
                String v = (String) propsToWrite.get(k);
                System.setProperty((String) k, v);
            }

        }

        return propsToWrite;
    }


    public static void StoreTheNewPropertiyFile(Properties propsToWrite, String PropertyFileName, File f) throws Exception {

        InputStream in = Application.class.getClassLoader().getResourceAsStream(PropertyFileName);
        Scanner inp = new Scanner(in);
        FileWriter wr = new FileWriter(f);
        while (inp.hasNextLine()) {
            String line = inp.nextLine().trim();

            if (line.indexOf("=") > 0 && line.startsWith("#") == false) {
                String[] list = line.split("=");
                //find value from the propert
                String Key = list[0].trim();
                Object newValue = propsToWrite.get(Key);

                String ValueStr = (newValue == null || newValue.equals("null") ? "" : newValue.toString());

                ////Dev-00003395 : extract the encrypted value for the password

                if (Key.toLowerCase().contains("password")) {
                    int start = ValueStr.toLowerCase().indexOf("enc(");
                    int end = ValueStr.lastIndexOf(")");
                    if (start < 0 || end < 0) {
                        if (ValueStr.trim().startsWith("${") && ValueStr.trim().endsWith("}") && ValueStr.contains(":")) {
                            int index = ValueStr.indexOf(":");
                            int LastIndex = ValueStr.indexOf("}");
                            String FirstPart = ValueStr.substring(0, index + 1);
                            String SecondPart = ValueStr.substring(index + 1, LastIndex);
                            ValueStr = FirstPart + "Enc(" + NTGEncryptor.encrypt(SecondPart) + ")}";
                        } else {
                            ValueStr = "Enc(" + NTGEncryptor.encrypt(ValueStr) + ")";
                        }
                    }
                }

                line = Key + "=" + ValueStr;
            }
            wr.write(line);
            wr.write("\r\n");
        }
        wr.close();
        inp.close();
        in.close();
        System.out.println("ReWrite Property File --> " + PropertyFileName);
    }

}
