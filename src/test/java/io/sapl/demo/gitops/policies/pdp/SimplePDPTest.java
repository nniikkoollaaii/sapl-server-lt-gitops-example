package io.sapl.demo.gitops.policies.pdp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.test.SaplTestFixture;
import io.sapl.test.integration.SaplIntegrationTestFixture;

class SimplePDPTest {

private SaplTestFixture fixture;
	
	@BeforeEach
	void setUp() {
		fixture = new SaplIntegrationTestFixture("policies");
	}
		
	@Test
	void test_verifyCombined_read() {

		fixture.constructTestCase()
			.when(AuthorizationSubscription.of("WILLI", "read", "foo"))
			.expectPermit()
			.verify();
			
	}
	
	@Test
	void test_verifyCombined_write() {

		fixture.constructTestCase()
			.when(AuthorizationSubscription.of("WILLI", "write", "foo"))
			.expectPermit()
			.verify();
			
	}
	
	
	
	

}
