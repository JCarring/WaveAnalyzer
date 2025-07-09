package com.carrington.WIA.stats;

/**
 * Calculates Fisher's Exact Test for 2x2 contingency tables. This test is used
 * to determine if there are nonrandom associations between two categorical
 * variables.
 */
public class FisherExact {

	private double[] f;
	private int maxSize;

	/**
	 * constructor for FisherExact table
	 *
	 * @param maxSize is the maximum sum that will be encountered by the table
	 *                (a+b+c+d)
	 */
	public FisherExact(int maxSize) {
		this.maxSize = maxSize;
		f = new double[maxSize + 1];
		f[0] = 0.0;
		for (int i = 1; i <= this.maxSize; i++) {
			f[i] = f[i - 1] + Math.log(i);
		}
	}

	/**
	 * Calculates the exact probability of observing a specific 2x2 table
	 * configuration, given the marginal totals.
	 *
	 * @param a cell count
	 * @param b cell count
	 * @param c cell count
	 * @param d cell count
	 * @return the P-value for this specific table configuration.
	 */
	private final double getP(int a, int b, int c, int d) {
		int n = a + b + c + d;
		if (n > maxSize) {
			return Double.NaN;
		}
		double p;
		p = (f[a + b] + f[c + d] + f[a + c] + f[b + d]) - (f[a] + f[b] + f[c] + f[d] + f[n]);
		return Math.exp(p);
	}

	/**
	 * Calculates the one-tail P-value for the Fisher Exact test. Determines whether
	 * to calculate the right- or left- tail, thereby always returning the smallest
	 * p-value.
	 *
	 * @param a cell count for the 2x2 matrix
	 * @param b cell count for the 2x2 matrix
	 * @param c cell count for the 2x2 matrix
	 * @param d cell count for the 2x2 matrix
	 * @return one-tailed P-value (right or left, whichever is smallest)
	 */
	public double getCumlativeP(int a, int b, int c, int d) {
		int min, i;
		int n = a + b + c + d;
		if (n > maxSize) {
			return Double.NaN;
		}
		double p = 0;
		p += getP(a, b, c, d);
		if ((a * d) >= (b * c)) {
			min = (c < b) ? c : b;
			for (i = 0; i < min; i++) {
				p += getP(++a, --b, --c, ++d);
			}
		}
		if ((a * d) < (b * c)) {
			min = (a < d) ? a : d;
			for (i = 0; i < min; i++) {
				double pTemp = getP(--a, ++b, ++c, --d);
				p += pTemp;
			}
		}
		return p;
	}

	/**
	 * Calculates the two-tailed P-value for the Fisher Exact test.
	 *
	 * In order for a table under consideration to have its p-value included in the
	 * final result, it must have a p-value less than or equal to the original
	 * table's P-value. This method sums the probabilities of all tables that are as
	 * extreme or more extreme than the observed table.
	 *
	 * @param a cell count for the 2x2 matrix
	 * @param b cell count for the 2x2 matrix
	 * @param c cell count for the 2x2 matrix
	 * @param d cell count for the 2x2 matrix
	 * @return two-tailed P-value or NaN if the table sum exceeds the maxSize.
	 *         Returns 1.0 if p is NaN.
	 */
	public double getTwoTailedP(int a, int b, int c, int d) {
		int min, i;
		int n = a + b + c + d;
		if (n > maxSize) {
			return Double.NaN;
		}
		double p = 0;

		double baseP = getP(a, b, c, d);

		int initialA = a, initialB = b, initialC = c, initialD = d;
		p += baseP;
		min = (c < b) ? c : b;
		for (i = 0; i < min; i++) {
			double tempP = getP(++a, --b, --c, ++d);
			if (tempP <= baseP) {
				p += tempP;
			}
		}

		// reset the values to their original so we can repeat this process for the
		// other side
		a = initialA;
		b = initialB;
		c = initialC;
		d = initialD;

		min = (a < d) ? a : d;
		for (i = 0; i < min; i++) {
			double pTemp = getP(--a, ++b, ++c, --d);
			if (pTemp <= baseP) {
				p += pTemp;
			}
		}
		return Double.isNaN(p) ? 1.0 : p;
	}

}