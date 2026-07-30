package ca.refundr.highspring.api.util;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportPathsTest {

	@Test
	public void usesExistingAbsoluteDirectory() throws Exception {
		Path dir = Files.createTempDirectory("highspring-allure");
		Path resolved = ReportPaths.resolve(dir.toAbsolutePath().toString());
		assertThat(resolved).isEqualTo(dir.toAbsolutePath().normalize());
	}

	@Test
	public void fallsBackToConfiguredPathWhenMissing() {
		Path resolved = ReportPaths.resolve("definitely-missing-report-dir-xyz");
		assertThat(resolved.getFileName().toString()).isEqualTo("definitely-missing-report-dir-xyz");
		assertThat(Files.isDirectory(resolved)).isFalse();
	}
}
