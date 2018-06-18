/*
 * Copyright (C) 2018 Martin Rumanek
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cz.cas.lib.proarc.common.export.sip;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.request.GetDatastreamDissemination;
import com.yourmediashelf.fedora.client.request.GetObjectXML;
import com.yourmediashelf.fedora.client.response.FedoraResponse;
import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.config.AppConfigurationFactory;
import cz.cas.lib.proarc.common.export.ExportUtils;
import cz.cas.lib.proarc.common.export.mets.MetsContext;
import cz.cas.lib.proarc.common.export.mets.MetsExportException;
import cz.cas.lib.proarc.common.export.mets.MetsUtils;
import cz.cas.lib.proarc.common.export.mets.NdkExport;
import cz.cas.lib.proarc.common.fedora.RemoteStorage;
import cz.cas.lib.proarc.common.fedora.SearchView;
import cz.cas.lib.proarc.common.object.DigitalObjectManager;
import cz.cas.lib.proarc.common.object.model.MetaModelRepository;
import cz.cas.lib.proarc.common.xml.SimpleNamespaceContext;
import cz.cas.lib.proarc.mets.info.Info;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class NdkSipExportTest {

    private static final int STATUS_OK = 200;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mocked
    private FedoraClient client;

    @Mocked
    private SearchView searchView;

    private RemoteStorage remoteStorage;

    private final AppConfiguration appConfig = AppConfigurationFactory.getInstance().defaultInstance();

    public NdkSipExportTest() throws Exception {
    }

    @Before
    public void setUp() {
        remoteStorage = new RemoteStorage(client);

        DigitalObjectManager.setDefault(new DigitalObjectManager(
                appConfig,
                null,
                remoteStorage,
                null,
                null));

        new MockUp<ExportUtils>() {
            @SuppressWarnings("EmptyMethod")
            @Mock
            void storeObjectExportResult(String pid, String target, String log) {
            }
        };

        new MockUp<FedoraClient>() {
            @Mock
            GetObjectXML getObjectXML(String pid) {
                return new GetObjectXML(pid) {
                    @Override
                    public FedoraResponse execute(FedoraClient fedora) {
                        return new FedoraResponse() {
                            @Override
                            public int getStatus() {
                                return STATUS_OK;
                            }

                            @Override
                            public InputStream getEntityInputStream() {
                                try {
                                    return getClass().getResource(StringUtils.remove(pid, "uuid:") + ".xml").openStream();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public <T> T getEntity(Class<T> c) {
                                return null;
                            }

                            @Override
                            public String getType() {
                                return null;
                            }

                            @Override
                            public void close() {
                            }
                        };
                    }
                };
            }

            @Mock
            GetDatastreamDissemination getDatastreamDissemination(
                    String pid, String dsId) {
                return new GetDatastreamDissemination(pid, dsId) {
                    @Override
                    public FedoraResponse execute(FedoraClient fedora) {
                        return new FedoraResponse() {
                            @Override
                            public int getStatus() {
                                return STATUS_OK;
                            }

                            @Override
                            public InputStream getEntityInputStream() {
                                try {
                                    Optional<URL> stream = Optional.ofNullable(getClass().getResource(dsId + "/" + StringUtils.remove(pid, "uuid:")));
                                    if (stream.isPresent()) {
                                        return stream.get().openStream();
                                    } else {
                                        URL objectUrl = getClass().getResource(StringUtils.remove(pid, "uuid:") + ".xml");
                                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                        dbf.setNamespaceAware(true);
                                        DocumentBuilder db = dbf.newDocumentBuilder();
                                        Document objectXML = db.parse(objectUrl.openStream());
                                        XPath xPath = XPathFactory.newInstance().newXPath();
                                        SimpleNamespaceContext namespaces = new SimpleNamespaceContext().add("foxml", "info:fedora/fedora-system:def/foxml#");
                                        xPath.setNamespaceContext(namespaces);
                                        Document datastreamXML = db.newDocument();
                                        Node node = (Node) xPath.compile("//foxml:datastream[@ID='BIBLIO_MODS']").evaluate(objectXML, XPathConstants.NODE);
                                        Node importedNode = datastreamXML.importNode(node, true);
                                        datastreamXML.appendChild(importedNode);

                                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        Source xmlSource = new DOMSource(datastreamXML);
                                        Result outputTarget = new StreamResult(outputStream);
                                        TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
                                        return new ByteArrayInputStream(outputStream.toByteArray());
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public <T> T getEntity(Class<T> c) {
                                return null;
                            }

                            @Override
                            public String getType() {
                                return null;
                            }

                            @Override
                            public void close() {
                            }
                        };
                    }
                };
            }

        };

        MetaModelRepository.setInstance("ndk", "ndkEborn");
    }

    @Test
    //TODO-MR test missing media (pdf)
    public void export() throws Exception {
        NdkExport export = new NdkSipExport(remoteStorage, appConfig.getNdkExportOptions());

        String pid = "uuid:acd66301-4e75-4d12-9d98-b323ff5beee9";

        assertTrue("Junit didn't create a temporary folder", folder.getRoot().exists());

        List<NdkExport.Result> resultsList = export.export(folder.getRoot(), Collections.singletonList(pid),
                true, true, null);

        for (NdkExport.Result result : resultsList) {
            if (result.getValidationError() != null) {
                throw result.getValidationError();
            }
        }

        String packageId = "123";
        Path sip = folder.getRoot().toPath().resolve(StringUtils.removeStart(pid, "uuid:")).resolve(packageId);
        validatePackage(sip);

        Files.walkFileTree(sip, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                System.out.println(file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    @Ignore
    //TODO-MR test multipart with multiple chapter as well
    public void exportMultipartMonograph() throws Exception {
        NdkExport export = new NdkSipExport(remoteStorage, appConfig.getNdkExportOptions());
        String pid = "uuid:26342028-12c8-4446-9217-d3c9f249bd13";
        List<NdkExport.Result> resultsList = export.export(folder.getRoot(), Collections.singletonList(pid),
                true, true, null);

        for (NdkExport.Result result : resultsList) {
            if (result.getValidationError() != null) {
                MetsExportException.MetsExportExceptionElement exception = result.getValidationError().getExceptions().get(0);
                fail(exception.getMessage() + " " + exception.getPid());
            }
        }

        String packageId = "123";
        Path sip = folder.getRoot().toPath().resolve(StringUtils.removeStart(pid, "uuid:")).resolve(packageId);

        Files.walkFileTree(sip, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                System.out.println(file);
                return FileVisitResult.CONTINUE;
            }
        });


        // validatePackage(sip);
    }

    private void validatePackage(Path sip) throws Exception {
        assertTrue("No SIP package", Files.isDirectory(sip));

        String identifier = sip.getFileName().toString();

        assertTrue("No original files", Files.list(sip.resolve("original")).count() > 0);
        assertTrue("No metadata files", Files.list(sip.resolve("metadata")).count() > 0);
        assertTrue("No info.xml", Files.exists(sip.resolve("info_" + identifier + ".xml")));
        assertTrue("No pdf file", Files.exists(sip.resolve("original/oc_" + identifier + ".pdf")));
        assertTrue("Empty pdf file", Files.size(sip.resolve("original/oc_" + identifier + ".pdf")) > 0);
        assertTrue("No mods file", Files.exists(sip.resolve("metadata/mods_volume.xml")));


        List<String> errors = MetsUtils.validateAgainstXSD(sip.resolve("info_" + identifier + ".xml").toFile(), Info.class.getResourceAsStream("info.xsd"));
        assertTrue(errors.toString(), errors.isEmpty());

        JAXBContext jContext = JAXBContext.newInstance(Info.class);
        Unmarshaller unmarshallerObj = jContext.createUnmarshaller();
        Info info = (Info) unmarshallerObj.unmarshal(sip.resolve("info_" + identifier + ".xml").toFile());
        assertTrue(info.getMetadataversion() >= NdkSipExport.PACKAGE_VERSION);
        assertEquals(info.getPackageid(), identifier);
        // assertEquals(info.getMainmets(), ""); //??? https://github.com/NLCR/Standard_NDK/issues/60

        assertTrue(!info.getTitleid().isEmpty());
        // assertTrue(!info.getCreator().isEmpty()); On Travis nullpointerexception

        assertTrue(info.getItemlist().getItem().size() > 1);
    }

    @Test
    public void findPSPPIDsTest() throws MetsExportException {
        MetsContext ctx = new MetsContext();
        ctx.setRemoteStorage(remoteStorage);
        ctx.setFedoraClient(remoteStorage.getClient());
        List<String> pids = MetsUtils.findPSPPIDs("uuid:acd66301-4e75-4d12-9d98-b323ff5beee9", ctx, true);
        assertTrue(pids.size() > 0);
    }
}