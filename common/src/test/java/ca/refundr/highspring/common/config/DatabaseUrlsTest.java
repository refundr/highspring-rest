package ca.refundr.highspring.common.config;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseUrlsTest {

	@Test
	public void convertsPostgresUrlToJdbc() {
		assertThat(DatabaseUrls.toJdbcUrl("postgresql://user:secret@dpg-abc.render.com/highspring"))
			.isEqualTo("jdbc:postgresql://dpg-abc.render.com:5432/highspring");
	}

	@Test
	public void leavesJdbcUnchanged() {
		String jdbc = "jdbc:postgresql://localhost:5436/highspring";
		assertThat(DatabaseUrls.toJdbcUrl(jdbc)).isEqualTo(jdbc);
	}

	@Test
	public void extractsUserInfo() {
		String raw = "postgres://alice:s3cret@host.example/db";
		assertThat(DatabaseUrls.usernameFromUrl(raw)).contains("alice");
		assertThat(DatabaseUrls.passwordFromUrl(raw)).contains("s3cret");
	}
}
