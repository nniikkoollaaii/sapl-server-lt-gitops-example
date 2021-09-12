package io.sapl.demo.gitops.policies.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.test.SaplTestFixture;
import io.sapl.test.unit.SaplUnitTestFixture;

public class PolicyAUnitTests {
	
	private SaplTestFixture fixture;
	
	@BeforeEach
	void setUp() {
		fixture = new SaplUnitTestFixture("policy_A");
	}
	
	@Test
	void test_SinglePolicyA_reat() {

		fixture.constructTestCase()
			.when(AuthorizationSubscription.of("WILLI", "read", "foo"))
			.expectDeny()
			.verify();
			
	}
	
	@Test
	void test_SinglePolicyA_write() {

		fixture.constructTestCase()
			.when(AuthorizationSubscription.of("WILLI", "write", "foo"))
			.expectDeny()
			.verify();
			
	}
}
