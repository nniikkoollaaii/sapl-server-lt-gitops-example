package io.sapl.demo.gitops.policies.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.test.SaplTestFixture;
import io.sapl.test.unit.SaplUnitTestFixture;

public class PolicyBUnitTest {
	
	private SaplTestFixture fixture;
	
	@BeforeEach
	void setUp() {
		fixture = new SaplUnitTestFixture("policy_B");
	}
	

	@Test
	void testSinglePolicyB_read() {

		fixture.constructTestCase()
			.when(AuthorizationSubscription.of("WILLI", "read", "foo"))
			.expectPermit()
			.verify();
			
	}
	
	@Test
	void testSinglePolicyB_write() {

		fixture.constructTestCase()
			.when(AuthorizationSubscription.of("WILLI", "write", "foo"))
			.expectPermit()
			.verify();
			
	}
}
