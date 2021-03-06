package org.owasp.dependencycheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.utils.Settings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mark Rekveld
 */
public class EngineModeIT extends BaseTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    @Rule
    public TestName testName = new TestName();

    @Before
    public void setUp() throws Exception {
        Settings.setString(Settings.KEYS.DATA_DIRECTORY, tempDir.newFolder().getAbsolutePath());
    }

    @Test
    public void testEvidenceCollectionAndEvidenceProcessingModes() throws Exception {
        List<Dependency> dependencies;
        try (Engine engine = new Engine(Engine.Mode.EVIDENCE_COLLECTION)) {
            assertDatabase(false);
            for (AnalysisPhase phase : Engine.Mode.EVIDENCE_COLLECTION.getPhases()) {
                assertThat(engine.getAnalyzers(phase), is(notNullValue()));
            }
            for (AnalysisPhase phase : Engine.Mode.EVIDENCE_PROCESSING.getPhases()) {
                assertThat(engine.getAnalyzers(phase), is(nullValue()));
            }
            File file = BaseTest.getResourceAsFile(this, "struts2-core-2.1.2.jar");
            engine.scan(file);
            engine.analyzeDependencies();
            dependencies = engine.getDependencies();
            assertThat(dependencies.size(), is(1));
            Dependency dependency = dependencies.get(0);
            assertTrue(dependency.getVendorEvidence().toString().toLowerCase().contains("apache"));
            assertTrue(dependency.getVendorEvidence().getWeighting().contains("apache"));
            assertTrue(dependency.getVulnerabilities().isEmpty());
        }

        try (Engine engine = new Engine(Engine.Mode.EVIDENCE_PROCESSING)) {
            assertDatabase(true);
            for (AnalysisPhase phase : Engine.Mode.EVIDENCE_PROCESSING.getPhases()) {
                assertThat(engine.getAnalyzers(phase), is(notNullValue()));
            }
            for (AnalysisPhase phase : Engine.Mode.EVIDENCE_COLLECTION.getPhases()) {
                assertThat(engine.getAnalyzers(phase), is(nullValue()));
            }
            engine.setDependencies(dependencies);
            engine.analyzeDependencies();
            Dependency dependency = dependencies.get(0);
            assertFalse(dependency.getVulnerabilities().isEmpty());
        }
    }

    @Test
    public void testStandaloneMode() throws Exception {
        try (Engine engine = new Engine(Engine.Mode.STANDALONE)) {
            assertDatabase(true);
            for (AnalysisPhase phase : Engine.Mode.STANDALONE.getPhases()) {
                assertThat(engine.getAnalyzers(phase), is(notNullValue()));
            }
            File file = BaseTest.getResourceAsFile(this, "struts2-core-2.1.2.jar");
            engine.scan(file);
            engine.analyzeDependencies();
            List<Dependency> dependencies = engine.getDependencies();
            assertThat(dependencies.size(), is(1));
            Dependency dependency = dependencies.get(0);
            assertTrue(dependency.getVendorEvidence().toString().toLowerCase().contains("apache"));
            assertTrue(dependency.getVendorEvidence().getWeighting().contains("apache"));
            assertFalse(dependency.getVulnerabilities().isEmpty());
        }
    }

    private void assertDatabase(boolean exists) throws Exception {
        Path directory = Settings.getDataDirectory().toPath();
        assertThat(Files.exists(directory), is(true));
        assertThat(Files.isDirectory(directory), is(true));
        Path database = directory.resolve(Settings.getString(Settings.KEYS.DB_FILE_NAME));
        assertThat(Files.exists(database), is(exists));
    }
}
