package ca.refundr.highspring.api.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves admin report directories so both common launch styles work:
 * <ul>
 *   <li>IntelliJ / cwd {@code api/} → {@code published-allure}</li>
 *   <li>{@code java -jar} from repo root → {@code api/published-allure}</li>
 * </ul>
 */
public final class ReportPaths {

	private ReportPaths() {
	}

	/**
	 * @param configured value from {@code ALLURE_REPORT_DIR} / {@code JAVADOC_REPORT_DIR}
	 * @return first existing directory among the usual candidates, else the configured path
	 *         (so missing-report errors still show a sensible location)
	 */
	public static Path resolve(String configured) {
		List<Path> candidates = new ArrayList<>();
		Path direct = Path.of(configured);
		candidates.add(direct);
		candidates.add(Path.of("api").resolve(configured));
		if (configured.startsWith("api/") || configured.startsWith("api\\")) {
			candidates.add(Path.of(configured.substring(4)));
		}
		for (Path candidate : candidates) {
			if (Files.isDirectory(candidate)) {
				return candidate.toAbsolutePath().normalize();
			}
		}
		return direct.toAbsolutePath().normalize();
	}
}
