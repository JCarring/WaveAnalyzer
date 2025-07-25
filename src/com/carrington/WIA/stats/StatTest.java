package com.carrington.WIA.stats;

/**
 * An enumeration of the statistical tests available within the application.
 * Each test has a defined order for display purposes and a user-friendly name.
 */
@SuppressWarnings("javadoc")
public enum StatTest {

	STUDENT_T_TEST_ONETAIL(2, "Student T-Test (one tail)"), STUDENT_T_TEST_TWOTAIL(3, "Student T-Test (two tail)"),
	WELCH_T_TEST_ONETAIL(4, "Welch's T-Test (one tail)"), WELCH_T_TEST_TWOTAIL(5, "Welch's T-Test (two tail)"),
	MANN_WHITNEY(6, "Mann-Whitney test"), FISHER_EXACT_TWOTAIL(7, "Fisher Exact (Two tails)"),
	FISHER_EXACT_ONETAIL(8, "Fisher Exact (One tail)"),
	ZTEST_PROP_ONETAIL(9, "Two sample proportion Z test (One tail)"),
	ZTEST_PROP_TWOTAIL(10, "Two sample proportion Z test (Two tails)"), ONE_WAY_ANOVA(11, "One-way ANOVA"),
	CHI_SQUARE(12, "Chi-Square"), KOLMOGOROV_SMIRNOV(13, "Kolmogorov Smirnov Test");

	private final int order;
	private final String name;

	private StatTest(int order, String name) {
		this.order = order;
		this.name = name;
	}

	/**
	 * Gets the display order for the test.
	 * 
	 * @return The integer order value.
	 */
	public int getOrder() {
		return this.order;
	}

	/**
	 * Returns the user-friendly name of the statistical test.
	 * 
	 * @return The string name of the test.
	 */
	public String toString() {
		return this.name;
	}

}
